# Testcontainers: Kotlin + Spring Boot 4.x での統合テスト設定

## 現象

`@SpringBootTest` + Testcontainers を使う統合テストで、CI（ubuntu-latest）で以下のようなエラーが発生する：

```
java.lang.IllegalStateException: Failed to load ApplicationContext
  Caused by: UnsatisfiedDependencyException: Error creating bean 'accountController'
    Caused by: UnsatisfiedDependencyException: Error creating bean 'listAccountsQuery'
      Caused by: NoSuchBeanDefinitionException: No qualifying bean of type 'AccountMapper'
```

## 根本原因

Spring Boot 4.x において以下の API・機能が削除または動作不安定になった：

| 機能 | 状況 |
|---|---|
| `@ServiceConnection` | DataSource 起動タイミングが不安定 |
| `@DynamicPropertySource` | 全コンテキストロードと Bean 初期化順序問題が発生 |
| `@JdbcTest` / `@AutoConfigureTestDatabase` | **Spring Boot 4.x で削除（`spring-boot-test-autoconfigure` から除去）** |
| `ContainerDatabaseDriver`（JDBC URL 方式） | DataSource が作成されず MyBatis 自動設定がスキップされる |

また、`spring-boot-testcontainers` を依存に含めると `testcontainers:2.0.5` がコアに引き込まれ、
`postgresql/jdbc`（1.20.4）との API 非互換が発生する（詳細は APP-ADR-0011）。

### NoSuchBeanDefinitionException 連鎖の仕組み

DataSource が Spring コンテキストに存在しない場合：
1. MyBatis の `@ConditionalOnSingleCandidate(DataSource.class)` が失敗 → MyBatis 自動設定スキップ
2. `AccountMapper` Bean が未登録
3. `listAccountsQuery`（`AccountMapper` を注入）が作れない → UnsatisfiedDependencyException
4. `accountController` が作れない → UnsatisfiedDependencyException

## 対処（確定パターン）

`@TestConfiguration` で `PostgreSQLContainer` を直接起動し `DataSource` Bean を提供する。
`DataSourceAutoConfiguration` を除外して devcontainer の `db:5432` への接続試行を防ぐ。

### テストクラス

```kotlin
@SpringBootTest(
    properties = ["spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"]
)
@ActiveProfiles("integration-test")
@Transactional
class AccountRepositoryImplIntegrationTest {
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
    lateinit var repository: AccountRepositoryImpl
    // tests...
}
```

### `application-integration-test.properties`

```properties
# DataSource は @TestConfiguration で直接提供するため設定不要
spring.flyway.enabled=true
```

### なぜこれが機能するか

1. `DataSourceAutoConfiguration` を除外 → `db:5432`（devcontainer）への接続試行なし
2. `@TestConfiguration.postgresContainer()` → PostgreSQL 16 Alpine コンテナを起動
3. `@TestConfiguration.dataSource()` → コンテナの JDBC URL で HikariCP 接続プールを作成
4. MyBatis が `DataSource` Bean を検出 → `SqlSessionFactory` → `AccountMapper` 登録
5. JdbcClient が `DataSource` Bean から自動設定 → `AccountRepositoryImpl` 作成
6. Flyway が `DataSource` Bean を使ってマイグレーション実行
7. 全 Bean が正常に初期化されテスト実行

## 防止策

- `AccountRepositoryImplIntegrationTest.kt` が参照実装として機能する
- `test-rules.md` に推奨パターンを記載
- `APP-ADR-0011` としてバージョン固定の意思決定と出口条件を記録

## 過去の試み（効果なし）

| 試み | 結果 |
|---|---|
| `@Testcontainers` + `@Container` + `@ServiceConnection` | DataSource 検出タイミング問題で失敗 |
| `@JvmStatic` を `@Container` プロパティに追加 | 上記と同様 |
| `@ServiceConnection` を外し `@DynamicPropertySource` を使用 | Bean 初期化順序問題で失敗 |
| `@JdbcTest` + `@AutoConfigureTestDatabase` に変更 | Spring Boot 4.x で API 削除済みのためコンパイルエラー |
| `ContainerDatabaseDriver`（JDBC URL 方式） | DataSource が Spring コンテキストに登録されず失敗 |
| `spring-boot-testcontainers` 依存を除去のみ | `testcontainers:2.0.5` 問題は解消したが DataSource 問題は残存 |
