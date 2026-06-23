# Testcontainers: Kotlin + Spring Boot 4.x での統合テスト設定

## 現象

`@SpringBootTest` + `@Testcontainers` + `@ServiceConnection` を使う統合テストで、CI（またはローカル）で以下のようなエラーが発生する：

```
java.lang.IllegalStateException: Failed to load ApplicationContext
  Caused by: UnsatisfiedDependencyException
    Caused by: UnsatisfiedDependencyException
      Caused by: NoSuchBeanDefinitionException
```

## 原因

Spring Boot 4.x では `@ServiceConnection` によるコンテナ検出が不安定になることがある。`@ServiceConnection` は Spring Boot がコンテキスト起動前にコンテナを検出・起動する仕組みだが、Kotlin の `companion object` + JUnit 5 の `@Testcontainers` 拡張との組み合わせで、コンテナが起動する前に Spring コンテキストが初期化されてしまう場合がある。

コンテナが起動していないと `DataSource` の自動設定が失敗し、`JdbcClient` → `AccountRepositoryImpl` → `AccountRepository` の依存チェーンで `NoSuchBeanDefinitionException` が連鎖する。

## 対処（推奨パターン）

`@ServiceConnection` / `@Testcontainers` を使わず、`@DynamicPropertySource` で明示的にプロパティを上書きする：

```kotlin
@SpringBootTest
@ActiveProfiles("integration-test")
@Transactional
class MyIntegrationTest {
    companion object {
        private val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:16-alpine").also { it.start() }  // 明示的に起動

        @JvmStatic
        @DynamicPropertySource
        fun postgresProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
        }
    }
}
```

## 防止策

- `.claude/rules/test-rules.md` に Testcontainers 統合テストの推奨パターンを記載
- `AccountRepositoryImplIntegrationTest.kt` が参照実装として機能する

## 以前の試み（効果なし）

- `@JvmStatic` を `@Container` と同じ companion object プロパティに追加 → `@ServiceConnection` の検出タイミング問題は解消されなかった
