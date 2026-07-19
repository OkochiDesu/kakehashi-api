---
globs:
  - "**/*Test.kt"
---

# テストコード規約

`*Test.kt` ファイルを編集・作成するときに適用するルール。
詳細なテスト方針は [kdoc-and-test-policy.md](../../docs/conventions/kdoc-and-test-policy.md) を参照。

## 目次

- [アサーション](#アサーション)
- [TDD（テスト先行）](#tddテスト先行)
  - [コンパイルレベルの変更（private constructor化等）でのred確認](#コンパイルレベルの変更private-constructor化等でのred確認)
  - [Testcontainers統合テストのred確認（CI依存の制約）](#testcontainers統合テストのred確認ci依存の制約)
- [正常系テストの観点](#正常系テストの観点)
- [異常系テストの観点](#異常系テストの観点)
- [バリデーション変更時](#バリデーション変更時)
- [Testcontainers 統合テスト](#testcontainers-統合テスト)
  - [テストクラス](#テストクラス)
  - [設定ファイル（`application-integration-test.properties`）](#設定ファイルapplication-integration-testproperties)
- [テストクラス KDoc（テストケース目次）](#テストクラス-kdocテストケース目次)
  - [フォーマット](#フォーマット)
  - [ルール](#ルール)
- [テスト命名](#テスト命名)

## アサーション

- **`assert()` を使わない**: JVM の `-ea` フラグが無効のとき評価されず常に成功する。`assertEquals` / `assertTrue` / `assertThrows` / `assertNull` 等 JUnit5 アサーションを使うこと（pre-commit でも検出する）
- **`assertNull()` より `assertNull(actual)` の形式を使う**: メッセージ引数は省略してよいが、対象は明示する

## TDD（テスト先行）

新規実装・バグ修正ともに、修正前・修正後を問わず**すべてのテストケース**を対象に、以下の4ステップを順番に踏む。

1. 修正・追加する仕様に対するテストコードを作成する（既存テストの修正を含む）
2. そのテストコードを実行し、失敗（red）することを確認する（ロジックをまだ変更していないため）
3. ロジックを実装・修正する
4. `./gradlew test` でテストスイート全体を実行し、ステップ2で失敗していたテストを含め**全テストが通ること**を確認する（green）

「テストを書いた」だけではステップ1のみでTDDを満たしたことにならない。ステップ2の実行確認を
省略すると、アサーション自体が誤っていて常に成功するケース（テストとして機能していない）を
検出できない。またステップ4は新規・修正したテストだけでなく**テストスイート全体**を実行し、
既存テストへの回帰がないことも併せて確認すること（新規テストのみ緑になって既存テストを
壊していないかは、新規テストの実行だけでは分からないため）。

### コンパイルレベルの変更（private constructor化等）でのred確認

`data class` → 通常 `class` 化、コンストラクタのvisibility変更等、**変更対象クラスの
公開APIそのものを変える大規模リファクタリング**では、テストのみを追加した時点で
プロジェクト全体がコンパイルできないことがある。この場合:

- **コンパイルエラーは有効なred確認として扱ってよい**（アサーション実行前の失敗もTDDのredに含まれる）
- ただし、コンパイルエラーが「追加したテストが期待する新しい振る舞い（新設メソッド呼び出し・
  変更後のコンストラクタシグネチャ等）に起因するものである」ことを一度エラーメッセージで
  確認すること。無関係な既存コードの破損によるエラーとred確認を混同しない

### Testcontainers統合テストのred確認（CI依存の制約）

devcontainer では Testcontainers がローカル実行できない制約があるため
（[testcontainers-jvmstatic-kotlin.md](../../docs/troubleshooting/testcontainers-jvmstatic-kotlin.md)）、
統合テストのみが対象の変更では実装前のローカルred確認ができない。この場合:

- 単体テストで代替できる範囲は単体テストでred/green確認を完結させる
- 統合テストでしか検証できない振る舞い（DBスキーマ・MyBatis Mapperのマッピング等）は、
  「テストを先に書く」（test-first authorship）までを実装前に行い、red確認はCIでの実行結果で代替してよい

## 正常系テストの観点

- 状態遷移・戻り値が仕様通りであることを確認する
- **状態を変更する UseCase の正常系テストでは、`updatedAt` が更新されていること・`updatedBy` が操作者 ID になっていることを同じテストケース内で検証すること**（別テストに分離しない）
- バグ修正で「正常系の仕様を最初から検証しきれていなかった」と判明した場合は、新規テストを追加するのではなく既存の正常系テストを強化する

## 異常系テストの観点

- 楽観ロック競合: `update()` が 0件 → `OptimisticLockException`。`currentVersion` の値が正しいことまで検証すること（`findById()` 2回目呼び出しに別バージョンを返す `returnsMany` を使用）
- 権限エラー: `isAdmin = false` / `operatorIsAdmin = false` → `ForbiddenOperationException`
- ステータス遷移不正: `canTransitionTo()` が false → `InvalidStatusTransitionException`
- Not Found: 対象が存在しない → `AccountNotFoundException`
- 不正な入力値: 未定義の `roleCode` など → `IllegalArgumentException`

## バリデーション変更時

バリデーション挙動を変更した diff（`runCatching.getOrNull()` 廃止・例外スロー追加・型変換ロジック変更等）がある場合は、**同じ diff 内**にエラーパステストを追加・更新すること。

## Testcontainers 統合テスト

Testcontainers を使う `@SpringBootTest` 統合テストでは **`@ServiceConnection` を使う**こと。
Spring Boot が DataSource・Flyway・MyBatis を自動設定する（APP-ADR-0013）。

**注意**: `@JdbcTest` / `@AutoConfigureTestDatabase` は Spring Boot 4.x で削除済み。`@DynamicPropertySource` / `ContainerDatabaseDriver`（JDBC URL 方式）は使わない。

### テストクラス

```kotlin
@Testcontainers
@SpringBootTest
@ActiveProfiles("integration-test")
@Transactional
class MyIntegrationTest {
    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
    }

    @Autowired
    lateinit var repository: MyRepositoryImpl
    // tests...
}
```

### 設定ファイル（`application-integration-test.properties`）

```properties
# @ServiceConnection が DataSource・Flyway・MyBatis を自動設定するため接続設定は不要
spring.flyway.enabled=true
```

- `@ActiveProfiles("integration-test")` を付与し devcontainer の DB に接続しないようにすること
- `@Transactional` を付与してテスト間のデータ汚染を防ぐこと
- **ローカル実行制約**: devcontainer では Docker socket が `root:root` 権限のため Testcontainers が起動できない。DB 系統合テストは CI（GitHub Actions）で確認すること（詳細: [testcontainers-jvmstatic-kotlin.md](../../docs/troubleshooting/testcontainers-jvmstatic-kotlin.md)）

詳細: [testcontainers-jvmstatic-kotlin.md](../../docs/troubleshooting/testcontainers-jvmstatic-kotlin.md)

## テストクラス KDoc（テストケース目次）

**新規テストクラス作成時・既存テストクラスへのテスト追加時**、クラスの KDoc に以下のフォーマットで
テストケース一覧を記載・更新すること。**テスト追加と同一コミットで KDoc を更新すること**（drift 防止）。

### フォーマット

```
/**
 * XxxUseCase 単体テスト
 *
 * 設計書No：UC-XX
 * ADRNo：APP-ADR-XXXX
 *
 * ★★全体観点★★
 * このテストクラスが何を保証するか・なぜ存在するかを1〜3文で説明する。
 * UI直結・権限の根幹・楽観ロック等、重要度が伝わるように書く。
 *
 * 《観　点》[何の動作・仕様を確認するか]
 * 《テスト》正常系： [正常ケースのテストケース名]
 * 《テスト》異常系： [同じ観点の異常ケース（あれば）]
 *
 * 《観　点》[別の観点（異常系のみでもよい）]
 * 《テスト》異常系： [テストケース名]
 */
```

### ルール
- `★★正常系★★` / `★★異常系★★` のセクション分割は**使わない**。`《観　点》` 単位でグループ化し、同一観点内は正常系を先に、異常系を後に並べる
- 1つの `《観　点》` に複数の `《テスト》` をまとめてよい（同一観点の複数ケース）
- **`《テスト》` の記述はテストメソッド名（backtick 内）と完全一致させること**。`正常系：` / `異常系：` プレフィックスを含む場合はそのまま含める
  - 良い例: `《テスト》正常系： grantAdminRole=true で admin ロールが付与される`
  - 悪い例: `《テスト》grantAdminRole=true で admin ロールが付与される`（プレフィックス抜け）
  - 悪い例: `《テスト》正常系: grantAdminRole=true で admin ロールが付与される`（コロン種別違い）
- 正常系・異常系の区別がない場合（ArchUnit 等）は `★★ルール★★` 等の適切なヘッダーを使う

## テスト命名

テスト名は「`正常系/異常系： 条件 → 期待結果`」の形式で書く。
- 良い例: `` `正常系： grantAdminRole=true で admin ロールが付与される` ``
- 良い例: `` `異常系： operatorIsAdmin=false は ForbiddenOperationException` ``
