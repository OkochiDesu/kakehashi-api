# APP-ADR-0012: Testcontainers を 2.0.5 へ移行し @TestConfiguration 直接起動方式を採用

## 目次

[ステータス](#ステータス) / [日付](#日付) / [関連](#関連) / [背景](#背景) / [決定](#決定) / [代替案](#代替案) / [影響](#影響) / [今後の見直しポイント](#今後の見直しポイント)

## ステータス

- [ ] Proposed
- [ ] Accepted
- [x] Superseded → [APP-ADR-0013](APP-ADR-0013-Testcontainers統合テストをServiceConnection方式へ移行.md)
- [ ] Rejected

## 日付

2026-06-24

## 関連

- Supersedes: APP-ADR-0011（Testcontainers コアを 1.20.4 に固定し Spring Boot 4.x の BOM 管理（2.x）を回避）
- Superseded by: [APP-ADR-0013](APP-ADR-0013-Testcontainers統合テストをServiceConnection方式へ移行.md)（Testcontainers 統合テストを @ServiceConnection 方式へ移行）— DataSource 提供方式を @TestConfiguration 直接起動から @ServiceConnection 方式へ変更。Testcontainers コア 2.0.5 への移行（決定1・3）と devcontainer 環境変数（決定4）は本 ADR でも有効。
- 補完: APP-ADR-0004（永続化技術スタックの導入）— DB 統合テストの依存・CI 構成の記録を本 ADR で更新する。

## 背景

APP-ADR-0011 では、Spring Boot 4.x の BOM が引き込む Testcontainers コア `2.x` と、JDBC URL 委譲方式（`ContainerDatabaseDriver`）が依存する `1.20.4` 系 API との非互換を避けるため、コアを `1.20.4` に固定する方針を採っていた。

しかし devcontainer（Docker-outside-of-Docker）環境で統合テストを実行したところ、Testcontainers `1.20.4` が Docker デーモン情報の取得（`/info`）に失敗し、テストそのものが実行できない問題が判明した。Docker-outside-of-Docker では Ryuk（リソース回収コンテナ）がコンテナ側ネットワークに戻れず、また起動した PostgreSQL コンテナへのポートアクセスがコンテナ間 IP で解決できないことが原因である。

また DataSource 提供方式も、JDBC URL 委譲方式（`ContainerDatabaseDriver`）では Spring Boot 4.x で DataSource が Spring コンテキストに登録されず、MyBatis 自動設定がスキップされて `AccountMapper` Bean が未登録になる問題があった（[testcontainers-jvmstatic-kotlin.md](../troubleshooting/testcontainers-jvmstatic-kotlin.md)）。

APP-ADR-0011 の「今後の見直しポイント」で定めた出口条件（Spring Boot の BOM 管理へ戻す／固定バージョン引き上げ／標準接続方式への移行）に対応する形で、依存構成と DataSource 提供方式を改めて決定する必要が生じた。

## 決定

Testcontainers の依存構成と統合テストの DataSource 提供方式を次の方針に変更する（[build.gradle.kts](../../build.gradle.kts)、[.devcontainer/devcontainer.json](../../.devcontainer/devcontainer.json)）。

1. **Testcontainers コアを `2.0.5`（Spring Boot 4.x の BOM 管理）に移行する**: `1.20.4` 固定を解除し、`testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.5"))` を宣言して BOM 管理に揃える。
2. **DataSource 提供方式を `@TestConfiguration` で `PostgreSQLContainer` を直接起動する方式に変更する**: JDBC URL 委譲方式（`ContainerDatabaseDriver`）を廃止し、`@TestConfiguration` 内で `PostgreSQLContainer` を起動して `DataSource`（HikariCP）Bean を提供する。`DataSourceAutoConfiguration` は `@SpringBootTest` の `properties` 属性で除外する。これにより Spring Boot 4.x でも DataSource が確実にコンテキストへ登録され、MyBatis / Flyway / JdbcClient の自動設定が機能する。
3. **アーティファクト名を 2.x 形式に変更する**: Testcontainers 2.x ではアーティファクト名が変更されたため、`junit-jupiter` → `testcontainers-junit-jupiter`、`postgresql` → `testcontainers-postgresql` を使用する。
4. **devcontainer 環境では Ryuk 無効化とホスト上書きを `remoteEnv` に設定する**: Docker-outside-of-Docker の Ryuk 通信制限・ポートアクセス問題を回避するため、`.devcontainer/devcontainer.json` の `remoteEnv` に `TESTCONTAINERS_RYUK_DISABLED=true` と `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal`（Docker Desktop の host IP）を設定する。
5. **CI（GitHub Actions ubuntu-latest）は native Docker のため上記環境変数を設定しない**: native Docker 環境では Ryuk もポートアクセスも正常に機能するため、devcontainer 固有の環境変数は不要であり設定しない。

対象ファイル:

- [build.gradle.kts](../../build.gradle.kts)
- [.devcontainer/devcontainer.json](../../.devcontainer/devcontainer.json)
- [src/test/resources/application-integration-test.properties](../../src/test/resources/application-integration-test.properties)
- `src/test/kotlin/com/kakehashi/infrastructure/account/AccountRepositoryImplIntegrationTest.kt`

## 代替案

### 代替案A: `~/.testcontainers.properties` で `ryuk.disabled=true` を設定する

- 長所: 環境変数を `devcontainer.json` に追加せずユーザーホーム配下のファイルで完結する。
- 短所: Testcontainers `2.x` ではプロパティファイルによる `ryuk.disabled` 設定が機能せず、Ryuk 無効化が反映されない。`2.x` では環境変数 `TESTCONTAINERS_RYUK_DISABLED` が正式な手順であるため採用しない。

### 代替案B: APP-ADR-0011 の `1.20.4` 固定と JDBC URL 委譲方式を維持する

- 長所: 依存・テスト構成の変更が不要。
- 短所: `1.20.4` が devcontainer（Docker-outside-of-Docker）環境で Docker 情報取得（`/info`）に失敗し、統合テストが実行できない。JDBC URL 委譲方式は Spring Boot 4.x で DataSource が登録されず MyBatis 自動設定がスキップされる問題も残るため、現実的に運用できない。

### 代替案C: Testcontainers を使わず devcontainer の DB に直接接続して統合テストを実行する

- 長所: 追加依存・コンテナ起動が不要。
- 短所: テスト実行が devcontainer の DB 起動状態に依存し、CI での再現性・分離性が損なわれる。テスト単位でコンテナを起動する Testcontainers の利点（クリーンな状態・並列性）が得られないため採用しない。

## 影響

- `build.gradle.kts` の Testcontainers 依存は `testcontainers-bom:2.0.5` + 2.x アーティファクト名に移行済み。`1.20.4` 固定と `dependencyManagement` での上書きは不要になった。
- 統合テストは `@SpringBootTest`（`DataSourceAutoConfiguration` 除外）+ `@ActiveProfiles("integration-test")` + `@Transactional` + `@TestConfiguration` での `PostgreSQLContainer` 直接起動を確定パターンとする（[.claude/rules/test-rules.md](../../.claude/rules/test-rules.md) の Testcontainers 節で規約化済み）。新規の `@SpringBootTest` 統合テストも同方式に従う。
- `application-integration-test.properties` は DataSource を `@TestConfiguration` で直接提供するため接続設定を持たず、`spring.flyway.enabled=true` のみを保持する。
- devcontainer 環境でのみ `TESTCONTAINERS_RYUK_DISABLED` / `TESTCONTAINERS_HOST_OVERRIDE` が必要になる。`.devcontainer/devcontainer.json` を変更する際は、この 2 環境変数の維持を確認する。CI 環境（native Docker）ではこれらを設定しない。
- [docs/troubleshooting/testcontainers-jvmstatic-kotlin.md](../troubleshooting/testcontainers-jvmstatic-kotlin.md) の APP-ADR-0011 参照箇所を本 ADR を指すよう同一コミットで更新する。

## 今後の見直しポイント

- Spring Boot をアップグレードし BOM が管理する Testcontainers コアバージョンが変わった場合は、`@TestConfiguration` 直接起動方式との整合（API 非互換の有無）を確認する。
- Docker-outside-of-Docker 以外の実行環境（純粋な Docker-in-Docker・native Docker のみ）に統一された場合は、devcontainer 固有の `TESTCONTAINERS_RYUK_DISABLED` / `TESTCONTAINERS_HOST_OVERRIDE` の要否を再検討する。
- `@ServiceConnection` / `@DynamicPropertySource` が Spring Boot の将来バージョンで安定動作するようになった場合は、`@TestConfiguration` 直接起動方式から標準方式への移行を検討する。
