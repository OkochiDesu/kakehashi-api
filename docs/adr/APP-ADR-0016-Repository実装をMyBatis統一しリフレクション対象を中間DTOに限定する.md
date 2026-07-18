# APP-ADR-0016: Repository 実装を MyBatis に統一し、リフレクション対象をエンティティ本体ではなく中間 DTO に限定する

## 目次

[ステータス](#ステータス) / [関連](#関連) / [背景](#背景) / [決定](#決定) / [代替案](#代替案) / [影響](#影響) / [今後の見直しポイント](#今後の見直しポイント)

## ステータス

- [ ] Proposed
- [x] Accepted
- [ ] Superseded
- [ ] Rejected

## 日付

2026-07-18

## 関連

- Supersedes: なし
- Superseded by: なし
- 関連: [APP-ADR-0015](APP-ADR-0015-DDDエンティティは振る舞いを持つ通常classとして実装し値オブジェクトのdataclassと区別する.md)（`private constructor` を持つエンティティ設計。本 ADR の前提）、[APP-ADR-0004](APP-ADR-0004-永続化技術スタックの導入-Flyway-MyBatis-PostgreSQL.md)（MyBatis 採用）、[APP-ADR-0008](APP-ADR-0008-DDD-CQRSアーキテクチャ原則の採用.md)（CQRS 分離。Query 側の既存 DTO パターンとの関係）

## 背景

本リポジトリのアカウント・ロールドメインでは、永続化技術が Command 側と Query 側で分裂している。

- Command 側（[`AccountRepositoryImpl`](../../src/main/kotlin/com/kakehashi/infrastructure/account/AccountRepositoryImpl.kt)）は Spring `JdbcClient` を用い、`ResultSet` からエンティティを構築する手書きマッピングを行っている。
- Query 側（[`AccountMapper`](../../src/main/kotlin/com/kakehashi/infrastructure/account/AccountMapper.kt)）は MyBatis を用い、`data class` の中間 DTO（`AccountSummaryRow` / `AccountDetailRow` / `RoleRow`）へ直接マッピングしている。

この使い分けの理由を明文化した ADR がこれまで存在しなかった。[APP-ADR-0004](APP-ADR-0004-永続化技術スタックの導入-Flyway-MyBatis-PostgreSQL.md) は MyBatis 採用を規定するが Query 側の文脈に寄っており、Command 側の技術選定には触れていない。[APP-ADR-0008](APP-ADR-0008-DDD-CQRSアーキテクチャ原則の採用.md) は CQRS の大枠フローを規定するが、実装技術（どちらの側で何を使うか）までは規定していない。

この技術的分裂は、ドキュメントを事前に読んでいない開発者が「DB アクセスロジックがどのフォルダ・どの技術にあるか」を予測しづらくするという懸念がユーザーから提起された。同じアカウントドメインの永続化でありながら、読み取り方向によって `JdbcClient` と MyBatis を使い分けている状態は、技術選定の一貫性を欠く。

この課題に対し、ユーザーから次の解決策が提案された。Interface Adapter 層（`RepositoryImpl`）を「境界防波堤」とし、MyBatis 専用 DTO からドメインエンティティへの詰め替えを必ず `RepositoryImpl` 内で行うことで、Command 側も MyBatis 化できるのではないか、という提案である。

議論の当初、Command 側が `JdbcClient` を使う理由を「`private constructor` を持つエンティティ（[APP-ADR-0015](APP-ADR-0015-DDDエンティティは振る舞いを持つ通常classとして実装し値オブジェクトのdataclassと区別する.md)）と MyBatis のリフレクション自動バインディングが根本的に相性が悪いため」と説明した。しかしこの説明は不正確だった。**MyBatis が直接リフレクションで触れる対象を、エンティティ本体ではなく中間 DTO（永続化フィールドと 1:1 対応する `class` / `data class`）に限定すれば、この非互換性は解消できる**、というのがユーザーとの議論での結論である。実際、Query 側はすでにこの中間 DTO パターンで MyBatis のリフレクションと `private constructor` 制約を無関係に共存させている。

## 決定

Repository 実装を **MyBatis に統一**し、MyBatis がリフレクションで触れる対象を **エンティティ本体ではなく中間 DTO に限定** する。具体的には、ユーザー提示の以下の 3 層構造で Repository 実装を統一する。

```
MyBatis ←（リフレクションで）→ AccountRow（DTO、class/data class 可） ←（手書き変換、reconstruct()）→ Account（Entity、private constructor）
```

- MyBatis は `@Mapper` インターフェース経由で、中間 DTO（例: `AccountRow`）にのみリフレクションでマッピングする。`Account`（エンティティ本体）には一度も直接触れない。
- [`AccountRepositoryImpl`](../../src/main/kotlin/com/kakehashi/infrastructure/account/AccountRepositoryImpl.kt)（`AccountRepository` の具象クラス、Interface Adapter 層）が MyBatis の `@Mapper` を DI する。読み取り方向は `AccountRow` を `Account.reconstruct(...)` ファクトリで明示的にエンティティへ変換する（この変換はリフレクションではなく手書きコード）。書き込み方向は `Account` のフィールドを `AccountRow` に詰め替えてから MyBatis の insert/update へ渡す。
- この構成により Command 側も MyBatis で実装可能になり、Query 側（`AccountMapper` → `AccountSummaryRow` / `AccountDetailRow` / `RoleRow`）と技術スタックを統一できる。

`RepositoryImpl` が「境界防波堤」として MyBatis 専用 DTO ↔ ドメインエンティティの詰め替えを一手に引き受けるため、MyBatis は常に中間 DTO のみに触れ、[APP-ADR-0015](APP-ADR-0015-DDDエンティティは振る舞いを持つ通常classとして実装し値オブジェクトのdataclassと区別する.md) の `private constructor` 制約と衝突しない。

### 単一集約読み取り vs 集約をまたぐ読み取りの使い分け

この統一パターンが適用されるのは「**単一集約・全フィールド・ID セントリックな読み取り**」（例: `Account` 1 件をそのまま取得する、Command 操作の前提となる読み込み）である。一方、以下のケースは既存の Query 側 DTO パターン（ドメインエンティティを経由しない）を維持すべきであり、**本 ADR の対象外** とする。

- `GetAccountQuery` が使う `AccountDetailRow`: `accounts` と `account_roles` / `roles` を JOIN した結合ビューであり、ドメインの `Account` エンティティには `roles` フィールドが存在しない（[APP-ADR-0007](APP-ADR-0007-rolesをpermissionベースに再定義しvisibility_rulesを廃止.md) で権限は別集約として扱う設計のため）。無理に `Account` エンティティの形に詰め替えようとすると、エンティティの集約境界を広げるか、JOIN を分割して 2 回のクエリにするかを強いられ、どちらも望ましくない。
- `ListAccountsQuery` が使う `AccountSummaryRow`: 一覧表示に必要な一部フィールドのみを持つ部分射影であり、`Account` の全フィールドを無理に埋めてまでエンティティ化する意味がない。

つまり本 ADR の決定は、Query 側の CQRS 分離（[APP-ADR-0008](APP-ADR-0008-DDD-CQRSアーキテクチャ原則の採用.md)）を置き換えるものではない。「Command 側（および将来的に単一集約読み取りの Query ユースケースがあれば、そのケース）の実装技術を MyBatis に統一する」という位置づけである。集約をまたぐ読み取り・部分射影の読み取りは、引き続きドメインエンティティを経由しない Query 側 DTO パターンで実装する。

## 代替案

### 代替案A: 現状維持（Command = JdbcClient、Query = MyBatis の分裂を継続）— 却下

- 内容: Command 側 `JdbcClient` / Query 側 MyBatis の使い分けをそのまま継続する。
- 却下理由: 技術選定の理由が文書化されておらず、新しい開発者が「どの側でどの技術を使っているか」を推測に頼らざるを得ない。同一ドメインの永続化で技術が分裂している既存のミスマッチが放置され、DB アクセスロジックの所在の予測可能性が低いままになる。

### 代替案B: すべての読み取り（一覧・結合ビューも含む）を強制的にドメイン Entity 経由にする — 却下

- 内容: `AccountDetailRow`（ロール結合ビュー）や `AccountSummaryRow`（部分射影）も含め、あらゆる読み取りをドメイン `Account` エンティティに詰め替えて統一する。
- 却下理由: 上記「単一集約読み取り vs 集約をまたぐ読み取り」で説明したとおり、`AccountDetailRow`（ロール結合）や `AccountSummaryRow`（部分射影）はドメイン Entity の形と一致しない。無理に統一すると、集約境界の破壊（`Account` に `roles` を持ち込む）か、クエリ分割による性能劣化（結合ビューを複数クエリに分ける）を招く。Query 側の DTO パターンには合理的な存在理由があり、一律 Entity 経由に統一することは害が大きい。

## 影響

- 本 ADR は将来 Command 側を MyBatis 化する際の **設計指針** であり、**現状の Command 側実装（`JdbcClient`・手書きマッピング）を今すぐ変更するものではない**。実際のリファクタリングは [APP-ADR-0015](APP-ADR-0015-DDDエンティティは振る舞いを持つ通常classとして実装し値オブジェクトのdataclassと区別する.md) の `Account` エンティティリファクタリング（`data class` → 通常 `class`）と合わせて、別ブランチで後日実施する。
- 中間 DTO（`AccountRow` 等）の命名・配置は、既存の Query 側パターン（[`AccountMapper.kt`](../../src/main/kotlin/com/kakehashi/infrastructure/account/AccountMapper.kt) 内の `AccountSummaryRow` 等）に倣うこと。
- Command 側を MyBatis 化する際は、[`.claude/rules/mybatis-rules.md`](../../.claude/rules/mybatis-rules.md) のルール（`#{}` 使用・`@param` 必須・`<id>` タグ・`notNullColumn` 等）に従うこと。
- 実装ガイドとして、[`kotlin-implementer`](../../.claude/agents/kotlin-implementer.md) など `.claude/agents/` 配下の関連エージェント定義が本方針（Repository 実装は MyBatis 統一・中間 DTO へのリフレクション限定・`RepositoryImpl` での詰め替え）を反映しているか、Command 側の永続化リファクタリングに着手する際に確認し、必要なら更新すること。

## 今後の見直しポイント

- 単一集約読み取りのユースケースが Query 側にも登場した場合、そのケースを本 ADR のパターン（中間 DTO → `reconstruct()` → エンティティ）に含めるか、Query 側 DTO パターンに留めるかを、集約境界・性能要件を踏まえて判断する。
- MyBatis から別の永続化技術（JPA/Hibernate 等）へ移行する場合（[APP-ADR-0004](APP-ADR-0004-永続化技術スタックの導入-Flyway-MyBatis-PostgreSQL.md) の前提が変わる場合）は、リフレクション対象を中間 DTO に限定する本方針の妥当性を再評価する。
- 中間 DTO ↔ エンティティの手書き詰め替えのボイラープレートがドメイン数の増加に伴い過大なコストになった場合は、変換コードの共通化・生成等の緩和策を新規 ADR で検討する。
