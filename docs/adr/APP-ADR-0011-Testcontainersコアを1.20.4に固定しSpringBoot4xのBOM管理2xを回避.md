# APP-ADR-0011: Testcontainers コアを 1.20.4 に固定し Spring Boot 4.x の BOM 管理（2.x）を回避

## 目次

[ステータス](#ステータス) / [関連](#関連) / [背景](#背景) / [決定](#決定) / [代替案](#代替案) / [影響](#影響) / [今後の見直しポイント](#今後の見直しポイント)

## ステータス

- [ ] Proposed
- [ ] Accepted
- [x] Superseded by: [APP-ADR-0012](APP-ADR-0012-Testcontainersを2.0.5へ移行しTestConfiguration直接起動方式を採用.md)
- [ ] Rejected

## 日付

2026-06-24

## 関連

- Supersedes: なし
- Superseded by: [APP-ADR-0012](APP-ADR-0012-Testcontainersを2.0.5へ移行しTestConfiguration直接起動方式を採用.md)（Testcontainers を 2.0.5 へ移行し @TestConfiguration 直接起動方式を採用）
- 補完: APP-ADR-0004（永続化技術スタックの導入）— 同 ADR の見直しポイント「DB統合テスト（Testcontainers等）を導入した際は、依存・CI構成の変更を本ADRまたは実装関連の別ADRに記録する」を受け、本 ADR で Testcontainers の依存構成を記録する。

## 背景

APP-ADR-0004 では Testcontainers による DB 統合テストを「必要になった時点で追加する」とし、依存・CI 構成の変更は本 ADR または別 ADR に記録すると見直しポイントに明記していた（[APP-ADR-0004 影響・今後の見直しポイント](APP-ADR-0004-永続化技術スタックの導入-Flyway-MyBatis-PostgreSQL.md)）。

Step1 のリポジトリ統合テスト（`AccountRepositoryImplIntegrationTest`）導入にあたり Testcontainers を実際に追加したところ、Spring Boot 4.x が管理する BOM は Testcontainers コアの `2.x` 系を引き込むことが判明した。一方で `org.testcontainers:postgresql` / JDBC モジュール（`ContainerDatabaseDriver`）は `1.20.4` 系 API に依存しており、`2.x` コアと組み合わせると API 非互換が発生する。

このバージョン整合は将来 Spring Boot のアップグレード時に再発しうるため、固定方針とその根拠を記録する必要が生じた。

## 決定

Testcontainers の依存構成を次の方針で固定する（[build.gradle.kts](../../build.gradle.kts)）。

1. **コアバージョンを 1.20.4 に固定する**: `dependencyManagement` ブロックで `org.testcontainers:testcontainers:1.20.4` を明示し、Spring Boot 4.x の BOM が管理する `2.x` を上書きする。
2. **`spring-boot-testcontainers` は依存に含めない**: このスターターは Spring Boot 4.x 管理の `2.x` コアを引き込み、`postgresql` / JDBC モジュール（`1.20.4`）との API 非互換を招くため使用しない。
3. **Testcontainers 関連モジュールは BOM 経由で揃える**: `testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.4"))` を宣言し、`junit-jupiter` / `postgresql` をバージョン無指定で BOM に揃える。
4. **統合テストの DB 接続は Testcontainers JDBC URL 方式を採る**: `@ServiceConnection` / `@DynamicPropertySource` 等は Spring Boot 4.x で正常動作しないため、`ContainerDatabaseDriver` に JDBC URL を委譲する方式を統合テストの確定パターンとする（[testcontainers-jvmstatic-kotlin.md](../troubleshooting/testcontainers-jvmstatic-kotlin.md)、[.claude/rules/test-rules.md](../../.claude/rules/test-rules.md)）。

対象ファイル:

- [build.gradle.kts](../../build.gradle.kts)
- [src/test/resources/application-integration-test.properties](../../src/test/resources/application-integration-test.properties)
- `src/test/kotlin/com/kakehashi/infrastructure/account/AccountRepositoryImplIntegrationTest.kt`

## 代替案

### 代替案A: Spring Boot 4.x の BOM が管理する Testcontainers 2.x をそのまま使う

- 長所: BOM 任せでバージョン宣言が不要になり、依存記述が簡潔になる。
- 短所: `postgresql` / JDBC モジュール（`ContainerDatabaseDriver`）が `1.20.4` 系 API に依存しており、`2.x` コアとの API 非互換が発生する。統合テストが起動・接続段階で失敗するため採用できない。

### 代替案B: `spring-boot-testcontainers` スターターを使い `@ServiceConnection` で接続する

- 長所: Spring Boot 標準の統合手段で、接続設定の記述量が減る。
- 短所: このスターターが `2.x` コアを引き込み代替案A と同じ非互換を招く。加えて Spring Boot 4.x では `@ServiceConnection` / `@DynamicPropertySource` が正常動作せず、接続そのものが確立できない（[testcontainers-jvmstatic-kotlin.md](../troubleshooting/testcontainers-jvmstatic-kotlin.md)）。

### 代替案C: Testcontainers を使わず devcontainer の DB に直接接続して統合テストを実行する

- 長所: 追加依存が不要。
- 短所: テスト実行が devcontainer の起動状態に依存し、CI での再現性・分離性が損なわれる。テスト間のデータ汚染管理も難しくなるため、コンテナをテスト単位で起動する Testcontainers を採用した。

## 影響

- `build.gradle.kts` の `dependencyManagement` ブロックと Testcontainers 依存記述（バージョン固定・固定理由のコメント）は本方針に沿って実装済み。Spring Boot の BOM を更新する際は、Testcontainers コアの `1.20.4` 固定との整合を必ず確認する。
- 統合テストは `@ActiveProfiles("integration-test")` + `@Transactional` + Testcontainers JDBC URL（`jdbc:tc:postgresql:...`）方式を前提とする。新規の `@SpringBootTest` 統合テストも同方式に従う（[.claude/rules/test-rules.md](../../.claude/rules/test-rules.md) の Testcontainers 節で規約化済み）。
- `application-integration-test.properties` は devcontainer の DB ではなく Testcontainers の JDBC URL を指す。統合テスト追加時にこのプロファイルを利用する。

## 今後の見直しポイント

- Spring Boot をアップグレードし、その BOM が管理する Testcontainers コアと `postgresql` / JDBC モジュールの API 互換が取れるようになった場合は、`1.20.4` 固定を解除して BOM 管理へ戻すか再検討する。
- Testcontainers が `2.x` 系で `postgresql` / JDBC モジュールの API 非互換を解消した場合は、固定バージョンの引き上げを検討する。
- `@ServiceConnection` / `@DynamicPropertySource` が Spring Boot の将来バージョンで正常動作するようになった場合は、JDBC URL 委譲方式から標準方式への移行を検討する。
