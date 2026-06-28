# Testcontainers: Kotlin + Spring Boot 4.x での統合テスト設定

## 目次

- [現象](#現象)
- [根本原因と解決経緯](#根本原因と解決経緯)
- [対処（確定パターン）](#対処確定パターン)
- [防止策](#防止策)
- [ローカル実行制約（devcontainer）](#ローカル実行制約devcontainer)
- [過去の試みと原因](#過去の試みと原因)

## 現象

`@SpringBootTest` + Testcontainers を使う統合テストで、CI（ubuntu-latest）で以下のようなエラーが発生する：

```
java.lang.IllegalStateException: Failed to load ApplicationContext
  Caused by: UnsatisfiedDependencyException: Error creating bean 'accountController'
    Caused by: UnsatisfiedDependencyException: Error creating bean 'listAccountsQuery'
      Caused by: NoSuchBeanDefinitionException: No qualifying bean of type 'AccountMapper'
```

## 根本原因と解決経緯

Spring Boot 4.x において以下の API・機能が削除または変更された：

| 機能 | 状況 |
|---|---|
| `@ServiceConnection` | **`spring-boot-testcontainers` 依存追加 + `companion object @JvmStatic` 配置で安定動作（APP-ADR-0013 で確定）** |
| `@DynamicPropertySource` | 全コンテキストロードと Bean 初期化順序問題が発生 |
| `@JdbcTest` / `@AutoConfigureTestDatabase` | **Spring Boot 4.x で削除（`spring-boot-test-autoconfigure` から除去）** |
| `ContainerDatabaseDriver`（JDBC URL 方式） | DataSource が作成されず MyBatis 自動設定がスキップされる |

当初 `@ServiceConnection` は `spring-boot-testcontainers` 依存なし・インスタンスフィールドへの配置で試行したため失敗した。
依存追加 + `companion object` 内 `@JvmStatic` 宣言により CI（GitHub Actions / native Docker）で安定動作を確認済み（2026-06-28）。
意思決定の記録: [APP-ADR-0013](../adr/APP-ADR-0013-Testcontainers統合テストをServiceConnection方式へ移行.md)（APP-ADR-0012 → APP-ADR-0011 を順次 Supersede）。

### NoSuchBeanDefinitionException 連鎖の仕組み

DataSource が Spring コンテキストに存在しない場合：
1. MyBatis の `@ConditionalOnSingleCandidate(DataSource.class)` が失敗 → MyBatis 自動設定スキップ
2. `AccountMapper` Bean が未登録
3. `listAccountsQuery`（`AccountMapper` を注入）が作れない → UnsatisfiedDependencyException
4. `accountController` が作れない → UnsatisfiedDependencyException

## 対処（確定パターン）

`@ServiceConnection` を `companion object` 内の `@JvmStatic` フィールドとして宣言する。
Spring Boot が `ConnectionDetails` 経由で DataSource・Flyway・MyBatis を自動設定する（APP-ADR-0013）。

### テストクラス

```kotlin
@Testcontainers
@SpringBootTest
@ActiveProfiles("integration-test")
@Transactional
class AccountRepositoryImplIntegrationTest {
    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
    }

    @Autowired
    lateinit var repository: AccountRepositoryImpl
    // tests...
}
```

### `application-integration-test.properties`

```properties
# @ServiceConnection が DataSource・Flyway・MyBatis を自動設定するため接続設定は不要
spring.flyway.enabled=true
```

### なぜこれが機能するか

1. `@JvmStatic` 付き `companion object` フィールドが static フィールドとして扱われ、Testcontainers JUnit 5 拡張がコンテキスト初期化前にコンテナを起動
2. `@ServiceConnection` が `JdbcConnectionDetails` / `FlywayConnectionDetails` 等の `ConnectionDetails` Bean を登録
3. Spring Boot が `ConnectionDetails` からコンテナの JDBC URL を取得し DataSource を自動設定
4. MyBatis が `DataSource` Bean を検出 → `SqlSessionFactory` → `AccountMapper` 登録
5. Flyway が `DataSource` Bean を使ってマイグレーション実行
6. 全 Bean が正常に初期化されテスト実行

## 防止策

- `AccountRepositoryImplIntegrationTest.kt` が参照実装として機能する
- `.claude/rules/test-rules.md` に推奨パターンを記載
- 意思決定履歴: `APP-ADR-0011` Superseded → `APP-ADR-0012` Superseded → [APP-ADR-0013](../adr/APP-ADR-0013-Testcontainers統合テストをServiceConnection方式へ移行.md)（現行）

## ローカル実行制約（devcontainer）

devcontainer 環境では Docker socket（`/var/run/docker.sock`）が `root:root` 権限で作成されるため、
`vscode` ユーザーから Testcontainers が Docker に接続できない。

**症状**: `Could not find a valid Docker environment.`

**対処**: DB 系統合テスト（`AccountRepositoryImplIntegrationTest` 等）は CI（GitHub Actions / native Docker）で確認する。
ローカルで Docker socket 権限を修正する場合は `sudo chmod 666 /var/run/docker.sock`（セッション限り）または devcontainer.json の `postCreateCommand` で恒久設定する。

## 過去の試みと原因

| 試み | 結果 | 原因 |
|---|---|---|
| `@Testcontainers` + `@Container` + `@ServiceConnection`（`spring-boot-testcontainers` 依存なし） | DataSource 検出タイミング問題で失敗 | 依存未追加のため `@ServiceConnection` が機能しない |
| `@JvmStatic` を `@Container` プロパティに追加（依存なし） | 上記と同様 | 同上 |
| `@ServiceConnection` を外し `@DynamicPropertySource` を使用 | Bean 初期化順序問題で失敗 | コンテキストロード順序の問題 |
| `@JdbcTest` + `@AutoConfigureTestDatabase` に変更 | Spring Boot 4.x で API 削除済みのためコンパイルエラー | API 削除 |
| `ContainerDatabaseDriver`（JDBC URL 方式） | DataSource が Spring コンテキストに登録されず失敗 | Spring Boot 4.x との非互換 |
| `spring-boot-testcontainers` 依存を除去のみ | コア引き込み問題は解消したが DataSource 問題は残存 | 依存削除後も根本原因（配置方法）は未解決 |
| **`spring-boot-testcontainers` 依存追加 + `companion object @JvmStatic` 配置** | **✅ 安定動作（APP-ADR-0013 で確定）** | — |
