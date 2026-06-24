# Testcontainers: Kotlin + Spring Boot 4.x での統合テスト設定

## 現象

`@SpringBootTest` + Testcontainers を使う統合テストで、CI（ubuntu-latest）で以下のようなエラーが発生する：

```
java.lang.IllegalStateException: Failed to load ApplicationContext
  Caused by: UnsatisfiedDependencyException
    Caused by: UnsatisfiedDependencyException
      Caused by: NoSuchBeanDefinitionException
```

## 根本原因

Spring Boot 4.x において以下の API・機能が削除または動作不安定になった：

| 機能 | 状況 |
|---|---|
| `@ServiceConnection` | DataSource 起動タイミングが不安定 |
| `@DynamicPropertySource` | Spring Boot 4.x の全コンテキストロードと組み合わせると Bean 順序問題が発生 |
| `@JdbcTest` / `@AutoConfigureTestDatabase` | **Spring Boot 4.x で削除（`spring-boot-test-autoconfigure` から除去）** |

`@SpringBootTest` は MyBatis・UseCase・Controller を含む全 Bean を起動する。DataSource の設定が失敗すると
`SqlSessionFactory`（MyBatis）が未定義になり、`AccountMapper` → `GetAccountQuery` / `ListAccountsQuery` の依存チェーンで
`NoSuchBeanDefinitionException` が連鎖する。

## 対処（確定パターン）

**Testcontainers JDBC URL** を `application-integration-test.properties` で設定する。
DataSource 作成時に `ContainerDatabaseDriver` が `jdbc:tc:postgresql:...` を検出してコンテナを自動起動する。
`@DynamicPropertySource` / `@Container` / companion object 不要。

### `application-integration-test.properties`

```properties
spring.datasource.url=jdbc:tc:postgresql:16-alpine:///testdb
spring.datasource.username=test
spring.datasource.password=test
spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver
spring.flyway.enabled=true
```

### テストクラス

```kotlin
@SpringBootTest
@ActiveProfiles("integration-test")
@Transactional
class AccountRepositoryImplIntegrationTest {
    @Autowired
    lateinit var repository: AccountRepositoryImpl
    // tests...
}
```

### なぜこれが機能するか

1. `@ActiveProfiles("integration-test")` により `application-integration-test.properties` がロードされる
2. `spring.datasource.url=jdbc:tc:postgresql:16-alpine:///testdb` を Spring Boot が解析
3. `ContainerDatabaseDriver` が起動 → PostgreSQL 16 Alpine コンテナを起動
4. 実際の PostgreSQL 接続 URL に差し替え → DataSource 作成成功
5. MyBatis / JdbcClient が正常に初期化 → 全 Bean が起動
6. Flyway マイグレーション実行 → テスト開始

## 防止策

- `application-integration-test.properties` で `ContainerDatabaseDriver` を使う
- `test-rules.md` に推奨パターンを記載
- `AccountRepositoryImplIntegrationTest.kt` が参照実装として機能する

## 過去の試み（効果なし）

| 試み | 結果 |
|---|---|
| `@JvmStatic` を `@Container` プロパティに追加 | `@ServiceConnection` の検出タイミング問題は未解消 |
| `@ServiceConnection` を外し `@DynamicPropertySource` を使用 | Spring Boot 4.x の Bean 初期化順序問題で失敗 |
| `@JdbcTest` + `@AutoConfigureTestDatabase` に変更 | Spring Boot 4.x でこれらの API が削除されコンパイルエラー |
