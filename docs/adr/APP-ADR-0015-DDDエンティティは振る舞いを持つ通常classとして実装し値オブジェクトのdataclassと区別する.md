# APP-ADR-0015: DDD エンティティは振る舞いを持つ通常 class として実装し、値オブジェクトの data class と区別する

## 目次

[ステータス](#ステータス) / [関連](#関連) / [背景](#背景) / [決定](#決定) / [代替案](#代替案) / [影響](#影響) / [今後の見直しポイント](#今後の見直しポイント)

## ステータス

- [ ] Proposed
- [x] Accepted
- [ ] Superseded
- [ ] Rejected

## 日付

2026-07-17

## 関連

- Supersedes: なし
- Superseded by: なし
- 関連: [APP-ADR-0008](APP-ADR-0008-DDD-CQRSアーキテクチャ原則の採用.md)（DDD/Clean Architecture 原則）、[APP-ADR-0005](APP-ADR-0005-楽観ロックにversionカラム整数カウンタを採用.md)（version 楽観ロック）、[APP-ADR-0004](APP-ADR-0004-永続化技術スタックの導入-Flyway-MyBatis-PostgreSQL.md)（MyBatis 採用）、[APP-ADR-0010](APP-ADR-0010-UseCaseのInputOutputをネストしたdataclassで定義しBuilderを使わない.md)（値/DTO には data class を使う場面の対比）、[APP-ADR-0016](APP-ADR-0016-Repository実装をMyBatis統一しリフレクション対象を中間DTOに限定する.md)（本 ADR の `private constructor` エンティティを前提とした Command 側 Repository の MyBatis 統一方針）

## 背景

アカウント集約のドメインエンティティ [`Account`](../../src/main/kotlin/com/kakehashi/domain/account/Account.kt) は、状態遷移メソッド（`register()` / `editName()` / `suspend()` / `unsuspend()`）を持つ Kotlin の `data class` として実装済みである。

一般的な DDD の議論では「エンティティを `data class`（構造的等価性を持つクラス）で実装すべきではない」という批判がある。エンティティは本来 ID による同一性（Identity）で判定されるべきで、`data class` が自動生成する全フィールドベースの `equals()` / `hashCode()`・`copy()`・`componentN()` は、値オブジェクト（Value Object）向けの性質だからである。実装者からこの懸念が提起されたため、本リポジトリの文脈で懸念が実際に問題となるかを事実ベースで調査した。その調査結果を踏まえ、`Account` を含む本リポジトリのエンティティ実装方針を意思決定として記録する。

代表的な 4 つの懸念について、以下を確認した。

1. **同一性(Identity)と等価性(Equality)の矛盾**
   `data class` の `equals()` / `hashCode()` は全フィールドに基づく構造的等価性であり、エンティティ本来の「ID のみで同一性を判定する」性質と矛盾しうる。
   調査結果: `Account` 同士を `==` / `equals()` で比較するコードは [`usecase/account/`](../../src/main/kotlin/com/kakehashi/usecase/account/)・[`infrastructure/account/`](../../src/main/kotlin/com/kakehashi/infrastructure/account/) 配下ともに 0 件（検出された `==` はいずれも `Int` の更新件数比較や文字列比較のみ）。`Set<Account>` / `Map<Account, _>` のようなハッシュコレクションのキー・要素としての利用も 0 件。テスト（[`AccountTest.kt`](../../src/test/kotlin/com/kakehashi/domain/account/AccountTest.kt)）も個別フィールドの `assertEquals` のみで、`Account` オブジェクト全体の比較はしていない。**現状は矛盾が顕在化する使い方をしていないが、これは「起きていない」だけであり「起きえない」わけではない。`data class` である限り、構造的等価性が同一性判定の意図と食い違う使い方をコンパイラが禁止できない。**

2. **状態変化と hashCode の崩壊**
   可変エンティティをハッシュコレクションに入れた後にフィールドを書き換えると、ハッシュバケットと矛盾する。
   調査結果: `Account` は全フィールド `val`（不変）で、状態遷移メソッドは全て新規インスタンスを返す設計（例: [`register()`](../../src/main/kotlin/com/kakehashi/domain/account/Account.kt)）。既存インスタンスを in-place で書き換える箇所は存在しない。不変設計（copy-on-write）のため、この懸念は構造的に発生し得ない。

3. **ORM（Hibernate 等）との相性**
   JPA/Hibernate はエンティティの差分検知（dirty checking）に基づき自動で UPDATE 文を生成するため、`data class` の構造的等価性・`hashCode` が SQL 自動生成ロジックと衝突しうる。
   調査結果: 本プロジェクトは MyBatis を採用しており JPA/Hibernate は不使用（[.claude/rules/mybatis-rules.md](../../.claude/rules/mybatis-rules.md)、[APP-ADR-0004](APP-ADR-0004-永続化技術スタックの導入-Flyway-MyBatis-PostgreSQL.md)）。[`AccountRepositoryImpl.update()`](../../src/main/kotlin/com/kakehashi/infrastructure/account/AccountRepositoryImpl.kt) は手書き SQL で `WHERE account_id = :accountId AND version = :prevVersion` により楽観ロックを実装している（[APP-ADR-0005](APP-ADR-0005-楽観ロックにversionカラム整数カウンタを採用.md)）。Entity Manager によるアタッチ/デタッチ管理や自動 dirty checking は存在しない。このため、この懸念は無関係である。

4. **HTTP 通信への影響**
   `data class` の自動生成メソッド（`copy` / `equals` / `toString`）が API レスポンスの契約に漏れ出すリスク。
   調査結果: [`AccountController`](../../src/main/kotlin/com/kakehashi/presentation/account/AccountController.kt) は `AccountId` / `RoleCode` のみを import しており、`Account`（エンティティ本体）は import していない。レスポンスは別の Response DTO 経由で構築されている（[APP-ADR-0008](APP-ADR-0008-DDD-CQRSアーキテクチャ原則の採用.md) の CQRS/DTO 境界分離）。ドメインエンティティが HTTP 境界を直接跨がないため、この懸念は該当しない。

さらに、上記 4 懸念とは別に、`data class` が公開生成する `copy()` によるビジネスルール迂回のリスクを確認した。

5. **`copy()` によるビジネスルール迂回**
   `data class` の `copy()` は常に public で生成される。`Account.suspend()` 等は内部で `check(status.canTransitionTo(...))` のようなガードを通すが、`copy()` 自体はクラス外から誰でも呼べるため、`account.copy(status = AccountStatus.ACTIVE)` のように直接呼び出せばステータス遷移ガードを完全に迂回できる。
   調査結果: 現状 `Account.copy(` を状態遷移メソッド外から呼び出している箇所は 0 件。**しかしこれも「迂回していない」だけであり、コンパイラレベルの防御がない。`data class` である限り、`copy()` によるガード迂回をコンパイラが禁止できない。**

以上より、懸念 2・3・4 は本リポジトリのアーキテクチャでは該当しないか構造的に発生し得ない。一方で懸念 1（同一性/等価性の矛盾）と懸念 5（`copy()` によるビジネスルール迂回）は、現状は顕在化していないものの、`data class` である限りコンパイラによる構造的な防御が存在せず、将来にわたり潜在リスクを抱え続ける。加えて `componentN()`（分解代入）によるエンティティ内部状態の外部露出リスクも残る。これらは「値オブジェクト向けの機能セット」を「エンティティ」に適用していることに起因する構造的なミスマッチである。

## 決定

本リポジトリで **DDD エンティティ（ドメイン集約のルートで振る舞いを持つクラス）は、`data class` ではなく通常の `class` として実装する**。この方針は `Account` に限定せず、今後実装予定の Resume・Skill 等のエンティティ（[exec-plans](../exec-plans/README.md) 0009・0010）を含む、本リポジトリのすべての DDD エンティティに一般方針として適用する。

値オブジェクト（`AccountId` 等の識別子、`AccountStatus` のような enum、UseCase の Input/Output や API の DTO など）は本方針の対象外であり、引き続き `data class` / `value class` / `enum class` で実装する。これらは構造的等価性（全フィールド一致で等価）が正しい意味を持つためである（[APP-ADR-0010](APP-ADR-0010-UseCaseのInputOutputをネストしたdataclassで定義しBuilderを使わない.md)）。

エンティティは以下のパターンで実装する（`Account` を例に示す。実際のリファクタリングは本 ADR のスコープ外で別途行う）。

```kotlin
class Account private constructor(
    val accountId: AccountId,
    val googleSubHash: String,
    val email: String,
    val name: String,
    val status: AccountStatus,
    val suspendedAt: OffsetDateTime?,
    val version: Int,
    val createdBy: String,
    val updatedBy: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
) {
    override fun equals(other: Any?): Boolean = other is Account && other.accountId == accountId
    override fun hashCode(): Int = accountId.hashCode()
    override fun toString(): String = "Account(accountId=$accountId, status=$status)" // email/googleSubHash を含めず PII 安全

    fun register(updatedBy: String): Account {
        check(status == AccountStatus.PROVISIONAL) { "register() は PROVISIONAL のみ実行可能です（現在のステータス: $status）" }
        return withChanges(status = AccountStatus.ACTIVE, updatedBy = updatedBy)
    }
    // suspend() / unsuspend() / editName() も同様のパターン

    private fun withChanges(
        status: AccountStatus = this.status,
        suspendedAt: OffsetDateTime? = this.suspendedAt,
        name: String = this.name,
        updatedBy: String = this.updatedBy,
    ): Account = Account(
        accountId, googleSubHash, email, name, status, suspendedAt,
        version + 1, createdBy, updatedBy, createdAt, OffsetDateTime.now(),
    )

    companion object {
        fun reconstruct(/* DB 行から再構築する全フィールド */): Account = Account(/* ... */)
    }
}
```

この設計により、背景で残存リスクとして確認した懸念を構造的に（コンパイラレベルで）解消する。

- **同一性/等価性の矛盾を解消（懸念 1）**: `equals()` / `hashCode()` を ID（`accountId`）のみで手書き実装する。エンティティ本来の「ID で同一性を判定する」性質と一致し、構造的等価性が漏れ出す将来リスクを設計時点で排除する。
- **ビジネスルール迂回を構造的に不可能にする（懸念 5）**: `data class` を名乗らないため `copy()` は自動生成されない。状態更新は `copy()` に似た書き心地の **手書き private ヘルパー `withChanges()`** で行う。公開された無条件更新メソッドが一切存在しないため、状態遷移ガードを迂回する余地が構造的になくなる。
- **直接構築の禁止**: `private constructor` + `companion object` のファクトリ関数（`reconstruct()` 等）により、クラス外からの不正な直接構築を禁止する。
- **内部状態の外部露出を防止**: `componentN()`（分解代入）が生成されないため、エンティティ内部状態が分解代入で外部露出するリスクも解消する（貧血ドメインモデル化の抑止）。
- **PII 露出対策**: `toString()` を明示的にオーバーライドし、`email` / `googleSubHash` 等の PII を含めず安全な内容（`accountId` / `status`）のみを出力する。

懸念 2・3・4 が本リポジトリで該当しないことは背景のとおりだが、それは「`data class` を維持してよい根拠」ではなく「通常 `class` へ移行しても失うものがない根拠」である。不変設計（全フィールド `val`・状態遷移は新規インスタンス生成）は通常 `class` でもそのまま維持する。

## 代替案

### 代替案A: `data class` のまま採用する（コンパイラ強制なし）— 却下

- 内容: `Account` を状態遷移メソッドを持つ不変 `data class` のまま維持する。懸念 1・5 は「現状は等価比較・ハッシュコレクション利用・`copy()` 迂回が 0 件」であることを根拠に許容する。
- 却下理由: 懸念 1・5 が現状顕在化していないのは事実だが、`data class` である限り、構造的等価性の漏出も `copy()` によるガード迂回もコンパイラが禁止できない。「起きていない」だけで「起きえない」わけではなく、将来の実装追加で容易に顕在化する。`data class` が提供する 4 点セット（`equals`/`hashCode`/`copy`/`componentN`）はいずれも値オブジェクト向けであり、エンティティにとっては同一性判定の破綻・ビジネスルール迂回・内部状態露出という潜在リスクにしかならない。コンパイラによる強制力を持つ通常 `class` を採る方が、将来にわたる安全性が高い。

### 代替案B: `data class` + `private constructor` + `equals`/`hashCode` 手書き上書き（折衷案）— 却下

- 内容: `data class` を維持しつつ、`private constructor` で `copy()` を隠蔽し、`equals()` / `hashCode()` を ID ベースで手書き上書きする。
- 却下理由: `data class` が提供する 4 点セットのうち 3 つ（`equals` / `hashCode` / `copy` の公開性）を無効化してまで `data class` を名乗ることになり、`data class` という宣言が読者に与える「これは値オブジェクトである」という意図のシグナルと実態が矛盾する。将来の読者に「なぜ `data class` なのに手書きで上書きしているのか」という無用な認知負荷を与える。加えて `componentN()`（分解代入）によるエンティティ内部状態の外部露出は残ったままで、貧血ドメインモデル化のリスクが解消されない。通常 `class` にすれば宣言と実態が一致し、これらの副作用も生じない。

いずれの代替案も、`data class` を維持することで得られる利点（`copy()` の記述簡潔さ）よりも、宣言と実態の乖離・コンパイラ強制の欠如・内部状態露出といった弊害が上回るため見送った。手書きの `withChanges()` ヘルパーで `copy()` 相当の書き心地は十分に確保できる。

## 影響

- 本 ADR 確定後、`Account` を含む本リポジトリの DDD エンティティは通常 `class` として実装する。VO（識別子・enum・DTO・UseCase Input/Output）は引き続き `data class` / `value class` / `enum class` を用い、エンティティと明確に区別する。
- **`Account` の実際のリファクタリング（`data class` → 通常 `class`）は本 ADR のスコープ外であり、別ブランチで後日実施する**。本 ADR は方針の確定のみを行い、`src/` 配下のコードは変更しない。
- エンティティの `equals()` / `hashCode()` は ID のみで判定する（構造的等価性ではない）。`accountId` が同一なら `version` / `updatedAt` 等が異なっても等価と判定される点に注意する。全体比較アサーション（`assertEquals(expectedAccount, actualAccount)`）を書く場合はこの性質を前提に、必要なら個別フィールドを個別に検証すること。
- エンティティは不変設計（全フィールド `val`・状態遷移は `withChanges()` 経由で新規インスタンスを返す）を維持する。in-place の書き換えメソッドを追加しないこと。
- 状態更新は手書き private ヘルパー（`withChanges()` 等）に集約し、公開された無条件更新メソッド（`copy()` 相当）を作らないこと。全ての状態遷移は `check(...)` ガードを通す公開メソッド経由に限定する。
- インスタンス生成は `private constructor` + `companion object` ファクトリ（新規生成用・DB 再構築用の `reconstruct()` 等）に限定すること。
- **Command 側 Repository 実装の技術選定への影響**: 本方針が目指す `private constructor` + ファクトリ（`reconstruct()` 等）を持つエンティティを Command 側 Repository でどう永続化するか（MyBatis 統一・中間 DTO へのリフレクション限定）は、[APP-ADR-0016](APP-ADR-0016-Repository実装をMyBatis統一しリフレクション対象を中間DTOに限定する.md) で独立した決定として記録する。本 ADR は「エンティティを通常 `class`・`private constructor` で実装する」方針のみを定め、Repository 実装技術の選定は APP-ADR-0016 に委ねる。
- `toString()` はエンティティごとに明示的にオーバーライドし、PII（`email` / `googleSubHash` 等）を出力しないこと。
- 今後実装するエンティティ（Resume・Skill 等、[exec-plans](../exec-plans/README.md) 0009・0010）は本方針に従って通常 `class` で実装する。
- 実装ガイドとして、[`kotlin-implementer`](../../.claude/agents/kotlin-implementer.md) など `.claude/agents/` 配下の関連エージェント定義が本方針を反映しているか、エンティティ実装に着手する際に確認し、必要なら更新すること。

## 今後の見直しポイント

- JPA/Hibernate 等の自動 dirty checking を伴う ORM を導入する場合（[APP-ADR-0004](APP-ADR-0004-永続化技術スタックの導入-Flyway-MyBatis-PostgreSQL.md) の前提が変わる場合）は、エンティティの `equals()` / `hashCode()` に対する ORM 側の要件（プロキシ・遅延ロード時の挙動等）を再評価し、本 ADR を見直す。
- 手書き `withChanges()` パターンのボイラープレートがエンティティ数の増加に伴い過大なコストになった場合は、コード生成・共通基底クラス等の緩和策を新規 ADR で検討する（ただし `copy()` 公開・構造的等価性の再導入は本 ADR の判断を覆すことになるため、その場合は本 ADR を Supersede する）。
- Kotlin 言語仕様の更新等により、`data class` でエンティティ向けの安全性（ID 等価性の強制・`copy()` の非公開化）が言語機能として得られるようになった場合は、本方針の再検討余地が生じる。
