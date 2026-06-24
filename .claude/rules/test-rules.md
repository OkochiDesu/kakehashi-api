---
globs:
  - "**/*Test.kt"
---

# テストコード規約

`*Test.kt` ファイルを編集・作成するときに適用するルール。
詳細なテスト方針は [kdoc-and-test-policy.md](../../docs/conventions/kdoc-and-test-policy.md) を参照。

## アサーション

- **`assert()` を使わない**: JVM の `-ea` フラグが無効のとき評価されず常に成功する。`assertEquals` / `assertTrue` / `assertThrows` / `assertNull` 等 JUnit5 アサーションを使うこと（pre-commit でも検出する）
- **`assertNull()` より `assertNull(actual)` の形式を使う**: メッセージ引数は省略してよいが、対象は明示する

## TDD（テスト先行）

新規実装・バグ修正ともに**テストを先に書いてから実装する**。
- バグ修正: バグを再現する失敗テストを書く → 修正する → テストが通ることを確認する
- 機能追加: 期待動作を定義するテストを書く → 実装する

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

Testcontainers を使う `@SpringBootTest` 統合テストでは **`@TestConfiguration` で `PostgreSQLContainer` を直接起動し `DataSource` Bean を提供する**こと。

**理由**: Spring Boot 4.x では `@JdbcTest` / `@AutoConfigureTestDatabase` / `@DynamicPropertySource` / `@ServiceConnection` / `ContainerDatabaseDriver`（JDBC URL 方式）のいずれも正常に動作しない。
`DataSourceAutoConfiguration` を除外し `@TestConfiguration` で DataSource を提供する方法が唯一の確定パターン。

### テストクラス

```kotlin
@SpringBootTest(
    properties = ["spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"]
)
@ActiveProfiles("integration-test")
@Transactional
class MyIntegrationTest {
    @TestConfiguration
    class TestDatasourceConfig {
        @Bean(destroyMethod = "stop")
        fun postgresContainer(): PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:16-alpine").also { it.start() }

        @Bean
        fun dataSource(postgres: PostgreSQLContainer<*>): DataSource =
            HikariDataSource(
                HikariConfig().apply {
                    jdbcUrl = postgres.jdbcUrl
                    username = postgres.username
                    password = postgres.password
                    driverClassName = "org.postgresql.Driver"
                }
            )
    }

    @Autowired
    lateinit var repository: MyRepositoryImpl
    // tests...
}
```

### 設定ファイル（`application-integration-test.properties`）

```properties
# DataSource は @TestConfiguration で直接提供するため設定不要
spring.flyway.enabled=true
```

- `@ActiveProfiles("integration-test")` を付与し devcontainer の DB に接続しないようにすること
- `@Transactional` を付与してテスト間のデータ汚染を防ぐこと

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
 * ★★正常系★★
 * 《観　点》[何の動作・仕様を確認するか]
 * 《テスト》[テストケース名（テストメソッド名と対応）]
 * 《テスト》[同じ観点で確認する別ケース]
 *
 * 《観　点》[別の観点]
 * 《テスト》[テストケース名]
 *
 * ★★異常系★★
 * 《観　点》[どのエラー境界・ガードを確認するか]
 * 《テスト》[テストケース名]
 * 《テスト》[同じ観点の別ケース]
 */
```

### ルール
- 1つの `《観　点》` に複数の `《テスト》` をまとめてよい（同一観点の複数ケース）
- `《テスト》` の記述はテストメソッド名（`` fun `正常系: ...` `` の backtick 内）と一致させること
- 正常系・異常系の区別がない場合（ArchUnit 等）は `★★ルール★★` 等の適切なヘッダーを使う

## テスト命名

テスト名は「`正常系/異常系： 条件 → 期待結果`」の形式で書く。
- 良い例: `` `正常系： grantAdminRole=true で admin ロールが付与される` ``
- 良い例: `` `異常系： operatorIsAdmin=false は ForbiddenOperationException` ``
