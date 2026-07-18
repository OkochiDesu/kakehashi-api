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
- 2026-07-18: PR #21 への Copilot レビュー指摘3件に対応（`feature/0006-google-sso-implementation` ブランチ）。
  - **`AuthStartupValidator` のドメイン検証不十分**: `allowedDomains.isNotBlank()` は `","` のようなカンマのみの値を通過させてしまい、`AccountUseCaseConfig` 側のパース結果（トリム・小文字化・空要素除外後）は空集合になる不整合があった。パースロジックを `AllowedDomainsParser`（config パッケージ、`internal object`）に集約し、`AccountUseCaseConfig` と `AuthStartupValidator` の双方から共用するようにした。`AuthStartupValidator.validate()` は `AllowedDomainsParser.parse(allowedDomains).isNotEmpty()` で判定する。
  - **JWT検証の空トークン500化対策**: jjwt はトークンが空文字列等の場合 `JwtException` 系ではなく `IllegalArgumentException`（`Assert.hasText` チェック）を投げることをソース確認済み。`JwtTokenIssuerImpl.verify()` の catch 対象に `IllegalArgumentException` を追加し `JwtVerificationFailedException` にラップするようにした（`JwtAuthenticationFilter` 経由で401に変換される）。
  - **未認証アクセスの403化対策**: `SecurityConfig` が `httpBasic()`/`formLogin()` を設定していないため、Spring Security のデフォルト `AuthenticationEntryPoint` が `Http403ForbiddenEntryPoint` にフォールバックし未認証アクセスが403になる問題があった。`RestAuthenticationEntryPoint`（`infrastructure.account` パッケージ、`@Component` なし）を新設し `HttpSecurity.exceptionHandling { it.authenticationEntryPoint(...) }` に明示登録した。`JwtAuthenticationFilter.writeUnauthorized` とレスポンス形式（JSON: code/message）を統一するため `writeUnauthorizedJson`（`AuthErrorResponseWriter.kt`）に共通化した。
  - テスト: `AuthStartupValidatorTest` にカンマのみのケースを追加、新規 `JwtTokenIssuerImplTest`（空文字列・不正形式トークンで `JwtVerificationFailedException` になることを確認）、`JwtAuthenticationFilterTest` に `Bearer `（空トークン）で401（500にならない）ケースを追加、新規 `RestAuthenticationEntryPointTest`（`commence()` 直接呼び出しで401・JSON形式を確認）。`RestAuthenticationEntryPoint` は `@WebMvcTest(AccountController::class)` がSecurity系オートコンフィグレーションを読み込まない（2026-07-03 の意思決定ログ参照）ため、フルの `SecurityFilterChain` を通した結合テストではなく `JwtAuthenticationFilterTest` と同じ方針（直接インスタンス化してのユニットテスト）で検証した。`./gradlew compileKotlin compileTestKotlin test` で確認済み（`AccountRepositoryImplIntegrationTest` は devcontainer の Docker socket 権限問題により既知の理由で失敗するが今回の変更とは無関係）。
- 2026-07-18: PR #21 への Copilot レビュー追加指摘3件に対応（`feature/0006-google-sso-implementation` ブランチ、`kotlin-implementer` 経由）。
  - **401レスポンスのJSON制御文字エスケープ漏れ**: `AuthErrorResponseWriter.writeUnauthorizedJson` の手書きエスケープ（`\`・`"` のみ）は `message` に改行・タブ等の制御文字が含まれると不正なJSONになる問題があった。手書きエスケープをやめ、Jackson の `ObjectMapper`（`tools.jackson.databind`、jackson-databind 3.1.2）で `UnauthorizedErrorBody`（`code`/`message` を持つ private data class）をシリアライズする方式に変更した。新規 `AuthErrorResponseWriterTest` で制御文字・バックスラッシュ・ダブルクォートを含むメッセージでも有効なJSONとして出力されることを確認した。
  - **Bearer スキームの大文字小文字区別・空白未トリム**: `JwtAuthenticationFilter.doFilterInternal` の `header.startsWith(BEARER_PREFIX)` が大文字小文字を区別しており `bearer <token>` のような正当なヘッダーを弾いていた。HTTPのauth-schemeは大文字小文字を区別しないため `startsWith(BEARER_PREFIX, ignoreCase = true)` に変更し、スキーム除去後のトークンを `trim()` してから検証するようにした。トリム後に空文字の場合は早期に401を返す（`JwtTokenIssuerImpl.verify()` 側の例外処理に頼らず意図を明確化）。`JwtAuthenticationFilterTest` に小文字スキーム・余分な空白・トリム後空文字の3ケースを追加した。
  - **Google Client IDの起動時検証漏れ**: `AuthStartupValidator` が `app.auth.jwt.secret`・`app.auth.google.allowed-domains` のみ検証しており、`app.auth.google.client-id`（`GoogleIdTokenVerifierImpl` の aud クレーム検証で使用）が空文字のまま起動しても検知できない問題があった（未設定だとGoogle SSOの全ログインが401になる）。`AuthStartupValidator` に `googleClientId`（`app.auth.google.client-id`、デフォルト空文字）のコンストラクタ引数を追加し、`validate()` に `googleClientId.isNotBlank()` のチェックを追加した。`AuthStartupValidatorTest` に空文字・空白のみの2ケースを追加した。
  - `./gradlew compileKotlin compileTestKotlin test` で確認済み（`AccountRepositoryImplIntegrationTest` は devcontainer の Docker socket 権限問題により既知の理由で失敗するが今回の変更とは無関係）。code-reviewer によるレビュー待ち。
- 2026-07-18: PR #21 への Copilot レビュー追加指摘4件に対応（`kotlin-implementer` 経由）。うち1件（`SecurityConfig` の `anyRequest().authenticated()` により、`AccountController` が本人特定・権限判定に使う `X-Account-Id`/`X-Is-Admin` ヘッダーとJWT principalが突合されておらず、有効なJWTを持つ任意の認証済みユーザーがヘッダー偽装で他人のaccountId・admin権限を名乗れる指摘）は、認可（ロールベースの権限判定）が exec-plan 0007 の既存スコープであるため本PRでは修正せず、exec-plan 0007 の DoD・タスク・意思決定ログに残課題として引き継いだ（ユーザー判断）。残り3件（`JwtAuthenticationFilter` のpermitAllバイパスバグ・`allowed-domains` 関連コメントの矛盾2件・KDoc規約とoverrideメソッド実装の矛盾）は本PRで修正済み。

## 残課題・引き継ぎ事項

- PR 作成・マージが未完了（次のステップ）。
- 認可（ロールベースの `@PreAuthorize` 等、UC-A1 以外のエンドポイントでの `SecurityContextHolder` 利用）は exec-plan 0007 のスコープ。JWTのprincipal（accountId）と`X-Account-Id`/`X-Is-Admin`ヘッダーの不整合によるなりすましリスク（PR #21 Copilot指摘、2026-07-18）を含め、詳細は[exec-plan 0007](../pending/0007-authorization-access-control.md)を参照。
- `GoogleIdTokenVerifierImpl` / `JwtTokenIssuerImpl` の infrastructure 層実装は、実際の Google JWKS 疎通を伴う統合テストを持たない（ユニットテストは `GoogleSsoCallbackUseCase` 側でポートをモックして検証、フィルターは自前JWTの実発行・検証のみ実体で検証）。Google 側との実疎通確認は手動確認 or 別途統合テストで補うことを検討する。
- `AuthStartupValidator`（`@Profile("!test & !integration-test")`）は、プロファイル未指定のフルコンテキスト `@SpringBootTest` では有効化される。現在 `@Disabled` の `KakehashiApiApplicationTests`（Testcontainers導入後に有効化予定）を再有効化する際は、`@ActiveProfiles("integration-test")` の指定または有効な `JWT_SECRET`/`GOOGLE_ALLOWED_DOMAINS` の設定が必要（未対応のまま再有効化するとコンテキストロードに失敗する）。
