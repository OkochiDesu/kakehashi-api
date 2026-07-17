# 0006: 認証基盤（Google SSO + JWT）

## 完了条件（Definition of Done）

- `POST /api/auth/google/callback`（UC-A1）が実装され、Google ID トークン検証・JIT プロビジョニングが動作する
- `SecurityContextHolder` から `accountId` を取得できる仕組みが確定している

## 目的・スコープ

Spring Security で Google SSO 認証を実装する。認可（exec-plan 0007）の基盤となるため先行する。

## 進捗状況

- [x] ADR 作成: JWT 戦略（自前発行 vs Google id_token Bearer）の決定（`adr-governance` 経由）→ APP-ADR-0014
- [x] 依存追加: `spring-boot-starter-security` / `spring-boot-starter-oauth2-resource-server` / JWT ライブラリ（jjwt-api 等）を `build.gradle.kts` に追加
- [x] API 設計書更新: `docs/design/api/account-role.md` の UC-A1 レスポンスへ JWT フィールド追加・認証節を APP-ADR-0014 に合わせて修正・参照 ADR リストへ APP-ADR-0014 を追加（`api-designer`）
- [x] テストシナリオ設計（`test-scenario-planner`）
- [x] Google ID トークン検証: `/api/auth/google/callback` で Google JWKS を用いて ID トークンを検証する仕組みの実装
- [x] 自前 JWT 発行ロジック実装: JWT 生成・署名・有効期限の実装（署名鍵の管理方針を決定し意思決定ログに記録する）
- [x] `GoogleSsoCallbackUseCase` の認証フロー実装（Google トークン検証 → JIT プロビジョニング → 自前 JWT 発行の統合。`AccountStatus.canLogin()` を仕様に合わせて修正することを含む）
- [x] 自前 JWT 検証カスタムフィルター実装: `Authorization: Bearer` の自前 JWT を検証し `SecurityContextHolder` に `accountId` をセットする（`@AuthenticationPrincipal` 等での取得確認を含む）
- [x] コードレビュー（`code-reviewer` → `test-reviewer`）
- [ ] PR 作成・マージ

## 意思決定ログ

- 2026-07-02: APP-ADR-0014 決定後、doc-maintainerチェックで判明したタスク粒度不足を解消するため進捗タスクを7項目→10項目に分解（依存追加・JWT発行ロジック・検証フィルターを独立タスク化）。
- 2026-07-02: 実装順序を決定。①依存追加 → ②API設計書更新・テストシナリオ設計（設計を先に固定） → ③Googleトークン検証 → ④自前JWT発行ロジック → ⑤`GoogleSsoCallbackUseCase`統合 → ⑥自前JWT検証フィルター（発行済みJWTが前提のため最後）→ ⑦コードレビュー → ⑧PR作成・マージ。
- 2026-07-03: 依存追加完了。`jjwt-api`/`jjwt-impl`/`jjwt-jackson` はバージョン 0.12.6 で固定（jjwt 0.12系はAPI/実装/Jacksonモジュールを分離する構成のため3点セットが必要）。あわせて `spring-security-test`（testImplementation）も追加した（後続のフィルター実装テストで `SecurityMockMvcRequestPostProcessors` 等を使うため）。既存の `AccountControllerTest` は `@WebMvcTest(AccountController::class)` によるスライステストであり、`@AutoConfigureMockMvc(addFilters = false)` 等の無効化設定は入っていない。全件PASSを確認したが、これはSpring Boot 4.x の `@WebMvcTest`（`org.springframework.boot.webmvc.test.autoconfigure`）が読み込むオートコンフィグレーション一覧（`AutoConfigureWebMvc.imports`）にSecurity系オートコンフィグレーションが含まれておらず、このスライスコンテキストにはSecurityFilterChainが構築されないためと確認した（jarを展開し実際のimportsファイルで検証済み）。後続タスクでカスタムの `SecurityFilterChain` Beanを定義した際、それがこのスライステストのコンポーネントスキャン対象に含まれる場合は前提が変わる可能性があるため、その時点で再確認する。
- 2026-07-08: `account-role.md`（UC-A1）を APP-ADR-0014 に合わせて更新完了（`api-designer`）。JWTレスポンスフィールド（`accessToken`）追加、認証・認可節の3段構成明記、参照ADRリストへのAPP-ADR-0014追加。
- 2026-07-08: `test-scenario-planner` によるUC-A1テストシナリオ設計完了。実装着手前の確認事項3点を決定: ①`AccountStatus.canLogin()` はUC-A1仕様（provisional/active→true、suspended/deactivated→false）に合わせて修正する（現状本番コード未使用のため影響なし）。②`GoogleSsoCallbackUseCase` はexec-planの記述通りGoogle検証→JIT→JWT発行の3段構成を統合する責務に変更する（Controller層での拒否判定は廃止）。③401/422用の新規例外クラスは提案通り `GoogleIdTokenVerificationException`（401）/ `InvalidIdTokenFormatException`（422）/ `DomainNotAllowedException`（422）を新設し、403は既存 `ForbiddenOperationException` を再利用する。
- 2026-07-11: 認証フロー本実装完了（`kotlin-implementer`）。主な実装・設計判断:
  - **署名鍵管理方針**: HMAC共有鍵（HS256）を採用（ADR-0014「影響」で実装時決定とされていた事項）。検証者がバックエンド自身のみのため非対称鍵より対称鍵で要件を満たせると判断。鍵は `app.auth.jwt.secret`（環境変数 `JWT_SECRET`）で注入し、`application.properties` のデフォルト値は開発/CI専用（本番は必ず上書き）。理由は `JwtTokenIssuerImpl` のKDocに記載。
  - **有効期限**: 60分（3600秒）固定。リフレッシュトークンは本実装では導入しない（ADR-0014で別途決定事項とされスコープ外）。運用上不便が顕在化したら再検討する。
  - **アーキテクチャ**: `GoogleIdTokenVerifier` / `JwtTokenIssuer` を domain 層のポート（インターフェース）として新設し、実装（Google JWKS 検証は `NimbusJwtDecoder`、自前JWTは `jjwt`）を infrastructure 層に配置。`ArchitectureTest` の依存方向制約（usecase/infrastructure は互いに依存しない）を満たすため、Google 検証失敗は domain 層例外 `GoogleIdTokenVerificationFailedException` を一度スローし、usecase 層（`GoogleSsoCallbackUseCase`）で捕捉して usecase 層例外（`GoogleIdTokenVerificationException` 等）に変換する2段構成にした。
  - **`JwtAuthenticationFilter`**: `@Component` を付与せず `SecurityConfig`（`config` パッケージ）から明示的にインスタンス化して `SecurityFilterChain` に登録した。`@Component` にすると `@WebMvcTest(AccountController::class)` のタイプベーススキャンで意図せず取り込まれ既存スライステストに影響するおそれがあるため（2026-07-03 の意思決定ログの継続検証）。
  - **許可ドメイン**: `app.auth.google.allowed-domains`（カンマ区切り、環境変数 `GOOGLE_ALLOWED_DOMAINS`）が空の場合はドメイン制限なし（開発環境向けデフォルト）。本番環境では必ず設定する運用とする。
- 2026-07-17: `code-reviewer` からのREQUIRES_CHANGES（3件）に対応（差し戻し修正）。
  - **JWT署名鍵のサイレントフォールバック対策**: `AuthStartupValidator`（config パッケージ、`@Component` + `@Profile("!test & !integration-test")`）を新設し、`app.auth.jwt.secret` が開発用デフォルト値のまま・32byte(256bit)未満のまま本番相当環境で起動しようとした場合に `IllegalStateException` を投げて起動を失敗させるfail-fastガードを追加した。同ガードで `app.auth.google.allowed-domains` が空のままの起動も失敗させる（指摘3も同時対応）。`test`/`integration-test` プロファイルでは無効化されるため既存テストへの影響はない。
  - **`email_verified` 未検証への対策**: `GoogleIdTokenVerifierImpl` に `email_verified` クレームの検証を追加し、true でない場合（false・未設定）は `GoogleIdTokenVerificationFailedException` をスローするようにした。ネットワーク疎通を要する署名検証（`NimbusJwtDecoder`）から切り離してユニットテスト可能にするため、デコード後の処理を `internal fun extractIdentity(jwt: Jwt)` として抽出した（`Jwt.withTokenValue(...).build()` で直接構築したテスト用トークンで検証）。
- 2026-07-17: `code-reviewer` 3回目レビューでAPPROVED（`AuthStartupValidator`のKDoc記述を`@Profile`の実際のセマンティクスに合わせて修正。`@WebMvcTest`が`@Component`を型スキャンしないことが既存テストで発火しない真の理由であることを明記）。続けて`test-reviewer`もAPPROVED（監査カラム・JWT異常系・fail-fastガード3ケース・email_verified3パターン・KDocテストケース目次の整合をすべて確認済み）。

## 残課題・引き継ぎ事項

- PR 作成・マージが未完了（次のステップ）。
- 認可（ロールベースの `@PreAuthorize` 等、UC-A1 以外のエンドポイントでの `SecurityContextHolder` 利用）は exec-plan 0007 のスコープ。
- `GoogleIdTokenVerifierImpl` / `JwtTokenIssuerImpl` の infrastructure 層実装は、実際の Google JWKS 疎通を伴う統合テストを持たない（ユニットテストは `GoogleSsoCallbackUseCase` 側でポートをモックして検証、フィルターは自前JWTの実発行・検証のみ実体で検証）。Google 側との実疎通確認は手動確認 or 別途統合テストで補うことを検討する。
- `AuthStartupValidator`（`@Profile("!test & !integration-test")`）は、プロファイル未指定のフルコンテキスト `@SpringBootTest` では有効化される。現在 `@Disabled` の `KakehashiApiApplicationTests`（Testcontainers導入後に有効化予定）を再有効化する際は、`@ActiveProfiles("integration-test")` の指定または有効な `JWT_SECRET`/`GOOGLE_ALLOWED_DOMAINS` の設定が必要（未対応のまま再有効化するとコンテキストロードに失敗する）。
