# APP-ADR-0008: DDD + CQRS アーキテクチャ原則の採用

## 目次

[ステータス](#ステータス) / [関連](#関連) / [背景](#背景) / [決定](#決定) / [代替案](#代替案) / [影響](#影響) / [今後の見直しポイント](#今後の見直しポイント)

## ステータス

- [ ] Proposed
- [x] Accepted
- [ ] Superseded
- [ ] Rejected

## 日付

2026-06-21

## 関連

- Supersedes: なし
- Superseded by: なし

## 背景

DDD（ドメイン駆動設計）& Clean Architecture と CQRS の徹底は、要件定義フェーズの段階から「アーキテクチャ原則（ADR化予定の方針）」として [docs/requirements/README.md 4章](../requirements/README.md#4-アーキテクチャ原則) に記述され、「確定後は `docs/adr/` にADRとして記録する」と明記されていた。

Step1の実装フェーズ（API設計 → Kotlin実装）が進行し、アカウント・ロールドメインのAPI設計書 [docs/design/api/account-role.md](../design/api/account-role.md) では、CQRSの適用を前提に Command（更新系）と Query（参照系）の処理フローを分離して設計済みである。設計書はその根拠として [docs/requirements/README.md 4章](../requirements/README.md#4-アーキテクチャ原則) を参照している。

また、永続化技術スタックを定めた [APP-ADR-0004](APP-ADR-0004-永続化技術スタックの導入-Flyway-MyBatis-PostgreSQL.md) でも、O/RマッパーにMyBatisを採用した理由として「参照系でJSONB/JOIN結果をDTOへ直接マッピングして高速に返すCQRS方針」を前提としており、本アーキテクチャ原則は既に複数の確定済み判断の土台となっている。

これらの方針は実装の全レイヤーに影響する横断的な決定であり、後続のドメイン設計・各ドメインのAPI設計・実装が同じ前提に従えるよう、確定したアーキテクチャ原則としてADRに記録する。

## 決定

kakehashi-api のバックエンドアーキテクチャ原則として、以下を採用する。

1. **DDD & Clean Architecture**
   ドメイン層（エンティティ、値オブジェクト、集約）はSpring/MyBatis等のフレームワークから完全に独立させる。永続化・Webの技術的関心事をドメイン層に持ち込まない（[docs/requirements/README.md 4章](../requirements/README.md#4-アーキテクチャ原則)）。

2. **CQRS の徹底**
   Command（更新系）と Query（参照系）を明確に分離する。

   | 分類 | 処理の流れ | 用途 |
   |---|---|---|
   | Command（更新系） | Controller → ドメイン集約 → Repository → DB | 状態の変更（イベント）を伴う操作 |
   | Query（参照系） | Controller → MyBatis Mapper（JOINクエリ）→ DTO → レスポンス | ドメイン層をバイパスし、JSONB/JOIN結果を直接DTOへマッピングして高速に画面へ返す |

   - Command は集約を経由し、状態遷移（イベント）を伴う。
   - Query はドメイン層・「イベント」「集約」を介在させず、MyBatisで直接DTOへマッピングする（[docs/requirements/README.md 4章](../requirements/README.md#4-アーキテクチャ原則)、[docs/design/api/account-role.md 設計方針](../design/api/account-role.md#cqrsの適用)）。

本ADRは、認証・認可方式（Google SSO + JIT、`@PreAuthorize` でのロール制御）や単一情報源（JSONB + PostgreSQL）といった隣接方針そのものを規定するものではなく、ドメイン設計・処理フローの原則（DDD/Clean Architecture と CQRS）を対象とする。認可方式は [APP-ADR-0007](APP-ADR-0007-rolesをpermissionベースに再定義しvisibility_rulesを廃止.md) ほかで、永続化技術は [APP-ADR-0004](APP-ADR-0004-永続化技術スタックの導入-Flyway-MyBatis-PostgreSQL.md) で扱う。

## 代替案

### 代替案A: レイヤードアーキテクチャ（トランザクションスクリプト型）で統一する

- 長所: 単純なCRUDでは実装がシンプルで学習コストが低い。
- 短所: 業務ルール（イベントストーミングで整理された集約・状態遷移）がService層に分散し、ドメインの不変条件を一貫して守りにくい。要件定義（[docs/requirements/README.md 7章](../requirements/README.md#7-イベントストーミング結果と業務ルール)）でイベント・集約を起点とした業務ルールが整理されており、これをドメイン層に集約できるDDD/Clean Architectureの方が適合する。

### 代替案B: 更新系・参照系とも単一のドメインモデル経由で処理する（CQRS不採用）

- 長所: モデルが一つで済み、Command/Queryでモデルを二重に持つ必要がない。
- 短所: 参照系（検索・一覧）でも集約のロード・組み立てを経由するため、JOIN/JSONBを活かした高速な画面応答が難しくなる。要件では検索画面の性能を重視しており（[docs/requirements/README.md 4章](../requirements/README.md#4-アーキテクチャ原則)）、Query側はMyBatisで直接DTOへマッピングするCQRSを採用した。

### 代替案C: 本方針をADR化せず要件定義ドキュメントの記述のみで運用する

- 長所: ドキュメントの追加が不要。
- 短所: 要件定義側で「確定後はADRに記録する」と明記されており、横断的な原則の根拠・代替案・見直し条件を時系列で残せない。後続のドメイン・API設計が参照する単一の決定記録が必要なため、ADR化した。

## 影響

- 以降の各ドメインのドメインモデル設計・API設計・実装は、本ADRの DDD/Clean Architecture と CQRS を前提とする。アカウント・ロールドメインの設計書 [docs/design/api/account-role.md](../design/api/account-role.md) は既にこの前提で記述済みである。
- Command 側はドメイン集約 → Repository、Query 側は MyBatis Mapper → DTO という2系統の実装が並存する。Query側で集約を経由しないことを許容する（CQRSの意図的なトレードオフ）。
- ドメイン層はフレームワーク非依存とするため、Entity/値オブジェクト/集約に Spring・MyBatis の型やアノテーションを持ち込まない実装規約となる。
- **パッケージ構成**: `domain/` / `usecase/` / `infrastructure/` / `presentation/` の4層で構成する。`service` という語は Spring の `@Service` と混同するため使用しない。UseCase クラスは `usecase/{context}/` に UC 単位で配置する。詳細は [docs/architecture/package-structure.md](../architecture/package-structure.md) を参照。
- **UseCase の DI 登録**: UseCase クラスには `@Service` を付与しない。POJO として実装し、`@Configuration` クラスの `@Bean` メソッドで DI コンテナに登録する。これによりドメイン・ユースケース層を Spring に依存させず、単体テストで Spring Context なしに `new UseCase(mockRepo)` でインスタンス化できる。
- **Enum の配置と活用**: `AccountStatus` / `RoleCode` 等のドメイン概念を表す Enum は `domain/{context}/` に配置する。ステータス遷移可否（`canTransitionTo`）・表示可否（`isSearchable`）等の業務ルールを Enum のメソッドとして実装し、UseCase・Controller への if/when 分散を防ぐ。
- ドメインモデル（エンティティ・値オブジェクト・集約の振る舞い）の詳細設計は、永続化層スキーマを対象とする [data-models.md](../requirements/data-models.md) とは別に実装フェーズで設計する（[docs/requirements/README.md 8章 2026-06-14 のログ](../requirements/README.md#8-意思決定ログ)）。

## 今後の見直しポイント

- CQRSのCommand/Query分離が運用上のオーバーヘッド（モデルの二重管理コスト等）に見合わないと判断された場合は、適用範囲の見直しを新規ADRで検討する。
- イベント駆動（ドメインイベントの非同期発行・イベントハンドラ）の実装方式を確定する際は、本ADRのCommand側方針との整合を確認し、必要なら別ADRで詳細化する。
