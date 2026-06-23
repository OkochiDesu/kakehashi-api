# Testcontainers: Kotlin companion object で @JvmStatic が必要

## 現象

`@SpringBootTest` + `@Testcontainers` + `@ServiceConnection` を使う統合テストで、CI（またはローカル）で以下のようなエラーが発生する：

```
java.lang.IllegalStateException: Failed to load ApplicationContext
  Caused by: UnsatisfiedDependencyException
    Caused by: UnsatisfiedDependencyException
      Caused by: NoSuchBeanDefinitionException
```

## 原因

Kotlin の `companion object` プロパティは、`@JvmStatic` を付与しない限り JVM 上で static フィールドにならない。JUnit 5 の `@Container` 拡張はクラスレベルライフサイクルを実現するために **static フィールド** を要求する。

`@JvmStatic` なしの場合、Testcontainers は `@Container` を インスタンスフィールドとして扱う。`@ServiceConnection` が Postgres コンテナを Spring コンテキスト起動前に登録できず、`DataSource` の自動設定が失敗する。その結果、`JdbcClient` → `AccountRepositoryImpl` → `AccountRepository` の依存チェーンで `NoSuchBeanDefinitionException` が連鎖する。

## 対処

```kotlin
companion object {
    @Container
    @ServiceConnection
    @JvmStatic  // ← これが必要
    val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
}
```

## 防止策

- `.githooks/pre-commit` が `@Container` あり `@JvmStatic` なしの `*Test.kt` ファイルを検出してコミットをブロックする（`src/test/kotlin/` 配下のみ対象）
- `.claude/rules/test-rules.md` に `@Container` + `@JvmStatic` 必須ルールを記載
