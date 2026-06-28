# APP-ADR-0013: Testcontainers 統合テストを @ServiceConnection 方式へ移行

## 目次

[ステータス](#ステータス) / [日付](#日付) / [関連](#関連) / [背景](#背景) / [決定](#決定) / [代替案](#代替案) / [影響](#影響) / [今後の見直しポイント](#今後の見直しポイント)

## ステータス

- [ ] Proposed
- [x] Accepted
- [ ] Superseded
- [ ] Rejected

## 日付

2026-06-28

## 関連

- Supersedes: [APP-ADR-0012](APP-ADR-0012-Testcontainersを2.0.5へ移行しTestConfiguration直接起動方式を採用.md)（Testcontainers を 2.0.5 へ移行し @TestConfiguration 直接起動方式を採用）
- Superseded by: なし
- 補完: APP-ADR-0004（永続化技術スタックの導入）— DB 統合テストの依存・接続方式の記録を本 ADR で更新する。

## 背景

APP-ADR-0012 では、Spring Boot 4.x で `@ServiceConnection` / `@DynamicPropertySource` / `ContainerDatabaseDriver`（JDBC URL 方式）が DataSource をコンテキストに登録できないと判断し、`@TestConfiguration` 内で `PostgreSQLContainer` を直接起動して `DataSource`（HikariCP）Bean を提供する方式を確定パターンとしていた。

その後、`spring-boot-testcontainers` 依存（`org.springframework.boot:spring-boot-testcontainers`）を追加したうえで、`companion object` 内の `@Container @ServiceConnection @JvmStatic` フィールドとして `PostgreSQLContainer` を宣言する標準パターンを検証したところ、Spring Boot 4.x + Testcontainers 2.0.5 の組み合わせで DataSource・Flyway・MyBatis の自動設定が正しく機能することを CI（GitHub Actions / native Docker）で確認できた。APP-ADR-0012 が「機能しない」とした原因は、`@ServiceConnection` フィールドを `@JvmStatic` の companion object に置かず、`spring-boot-testcontainers` 依存も導入していなかったことにあったと判明した。

`@TestConfiguration` 直接起動方式は DataSource・接続情報を手動で組み立てる必要があり、Flyway・MyBatis の自動設定との整合をテストごとに保証する負担が残る。Spring Boot 標準の `@ServiceConnection` 方式に移行することで、接続情報の手動構築を排し、フレームワーク標準の自動設定に委ねられる。

APP-ADR-0012 の「今後の見直しポイント」で定めた「`@ServiceConnection` が安定動作するようになった場合は標準方式への移行を検討する」に対応する形で、統合テストの DataSource 提供方式を改めて決定する。

## 決定

統合テストの DataSource 提供方式を `@ServiceConnection` 方式に変更する（[build.gradle.kts](../../build.gradle.kts)、[.claude/rules/test-rules.md](../../.claude/rules/test-rules.md)）。

1. **`spring-boot-testcontainers` 依存を追加する**: `testImplementation("org.springframework.boot:spring-boot-testcontainers")` を宣言し、`@ServiceConnection` サポートを有効化する。
2. **DataSource 提供方式を `@ServiceConnection` 方式に変更する**: `@TestConfiguration` での `PostgreSQLContainer` 直接起動と HikariCP の手動構築を廃止し、`companion object` 内に `@Container @ServiceConnection @JvmStatic` フィールドとして `PostgreSQLContainer` を宣言する。`DataSourceAutoConfiguration` の除外（`@SpringBootTest` の `properties` 属性）も不要になる。Spring Boot がコンテナの接続情報から DataSource・Flyway・MyBatis を自動設定する。
3. **テストクラスに `@Testcontainers` を付与する**: JUnit 5 拡張（`org.testcontainers.junit.jupiter.Testcontainers`）でコンテナのライフサイクルを管理する。`@SpringBootTest` + `@ActiveProfiles("integration-test")` + `@Transactional` は維持する。
4. **`application-integration-test.properties` は接続設定を持たない**: `@ServiceConnection` が DataSource・Flyway・MyBatis を自動設定するため、`spring.flyway.enabled=true` のみを保持する。

対象ファイル:

- [build.gradle.kts](../../build.gradle.kts)
- [src/test/resources/application-integration-test.properties](../../src/test/resources/application-integration-test.properties)
- `src/test/kotlin/com/kakehashi/infrastructure/account/AccountRepositoryImplIntegrationTest.kt`
- [.claude/rules/test-rules.md](../../.claude/rules/test-rules.md)

## 代替案

### 代替案A: APP-ADR-0012 の `@TestConfiguration` 直接起動方式を維持する

- 長所: 既存の参照実装・規約をそのまま使え、変更が不要。
- 短所: DataSource・接続情報の手動構築が残り、Flyway・MyBatis 自動設定との整合をテストごとに保証する負担がある。Spring Boot 標準の `@ServiceConnection` が動作することが確認できた以上、フレームワーク標準から外れた構成を維持する積極的理由がない。

### 代替案B: `@DynamicPropertySource` でコンテナの接続情報を Environment に注入する

- 長所: `spring-boot-testcontainers` 依存を追加せずに接続情報を渡せる。
- 短所: 接続プロパティのキーを手動で列挙する必要があり、`@ServiceConnection` のように DataSource・Flyway・MyBatis を一括で自動設定できない。標準の `@ServiceConnection` が動作する以上、より低水準な方式を選ぶ理由がない。

## 影響

- `build.gradle.kts` に `spring-boot-testcontainers` 依存を追加した。Testcontainers コアは APP-ADR-0012 で移行済みの `testcontainers-bom:2.0.5` + 2.x アーティファクト名を維持する。
- 統合テストは `@Testcontainers` + `@SpringBootTest` + `@ActiveProfiles("integration-test")` + `@Transactional` を付与し、`companion object` 内に `@Container @ServiceConnection @JvmStatic val postgres` を宣言する方式を確定パターンとする（[.claude/rules/test-rules.md](../../.claude/rules/test-rules.md) の Testcontainers 節で規約化済み）。新規の `@SpringBootTest` 統合テストも同方式に従う。
- `@ServiceConnection` フィールドは `companion object` 内に `@JvmStatic` を付けて宣言すること。インスタンスフィールドに置くと Spring がコンテキスト初期化時にコンテナを検出できない。
- `application-integration-test.properties` は接続設定を持たず、`spring.flyway.enabled=true` のみを保持する。`DataSourceAutoConfiguration` の除外指定も不要。
- **ローカル実行制約**: devcontainer 環境では Docker socket（`/var/run/docker.sock`）が `root:root` 権限で作成されるため、`vscode` ユーザーから Testcontainers が Docker に接続できない（`Could not find a valid Docker environment.`）。DB 系統合テストは CI（GitHub Actions / native Docker）で確認する。ローカルで確認する場合は Docker socket 権限の修正が必要（[docs/troubleshooting/testcontainers-jvmstatic-kotlin.md](../troubleshooting/testcontainers-jvmstatic-kotlin.md)）。
- [docs/troubleshooting/testcontainers-jvmstatic-kotlin.md](../troubleshooting/testcontainers-jvmstatic-kotlin.md) の APP-ADR-0012 参照箇所と「試行済みパターン」記述を本 ADR を指すよう同一コミットで更新する。

## 今後の見直しポイント

- Spring Boot をアップグレードし BOM が管理する Testcontainers コアバージョンが変わった場合は、`@ServiceConnection` 方式との整合（API 非互換の有無）を確認する。
- devcontainer の Docker socket 権限問題が `postCreateCommand` 等で恒久的に解消された場合は、ローカル実行制約の記述を見直す。
- `@ServiceConnection` が将来のバージョンで非推奨化された場合は、その時点の Spring Boot 標準パターンへの移行を検討する。
