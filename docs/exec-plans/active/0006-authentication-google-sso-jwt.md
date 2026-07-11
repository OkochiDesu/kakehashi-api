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
- [ ] Google ID トークン検証: `/api/auth/google/callback` で Google JWKS を用いて ID トークンを検証する仕組みの実装
- [ ] 自前 JWT 発行ロジック実装: JWT 生成・署名・有効期限の実装（署名鍵の管理方針を決定し意思決定ログに記録する）
- [ ] `GoogleSsoCallbackUseCase` の認証フロー実装（Google トークン検証 → JIT プロビジョニング → 自前 JWT 発行の統合。`AccountStatus.canLogin()` を仕様に合わせて修正することを含む）
- [ ] 自前 JWT 検証カスタムフィルター実装: `Authorization: Bearer` の自前 JWT を検証し `SecurityContextHolder` に `accountId` をセットする（`@AuthenticationPrincipal` 等での取得確認を含む）
- [ ] コードレビュー（`code-reviewer` → `test-reviewer`）
- [ ] PR 作成・マージ

## 意思決定ログ

- 2026-07-02: APP-ADR-0014 決定後、doc-maintainerチェックで判明したタスク粒度不足を解消するため進捗タスクを7項目→10項目に分解（依存追加・JWT発行ロジック・検証フィルターを独立タスク化）。
- 2026-07-02: 実装順序を決定。①依存追加 → ②API設計書更新・テストシナリオ設計（設計を先に固定） → ③Googleトークン検証 → ④自前JWT発行ロジック → ⑤`GoogleSsoCallbackUseCase`統合 → ⑥自前JWT検証フィルター（発行済みJWTが前提のため最後）→ ⑦コードレビュー → ⑧PR作成・マージ。
- 2026-07-03: 依存追加完了。`jjwt-api`/`jjwt-impl`/`jjwt-jackson` はバージョン 0.12.6 で固定（jjwt 0.12系はAPI/実装/Jacksonモジュールを分離する構成のため3点セットが必要）。あわせて `spring-security-test`（testImplementation）も追加した（後続のフィルター実装テストで `SecurityMockMvcRequestPostProcessors` 等を使うため）。既存の `AccountControllerTest` は `@WebMvcTest(AccountController::class)` によるスライステストであり、`@AutoConfigureMockMvc(addFilters = false)` 等の無効化設定は入っていない。全件PASSを確認したが、これはSpring Boot 4.x の `@WebMvcTest`（`org.springframework.boot.webmvc.test.autoconfigure`）が読み込むオートコンフィグレーション一覧（`AutoConfigureWebMvc.imports`）にSecurity系オートコンフィグレーションが含まれておらず、このスライスコンテキストにはSecurityFilterChainが構築されないためと確認した（jarを展開し実際のimportsファイルで検証済み）。後続タスクでカスタムの `SecurityFilterChain` Beanを定義した際、それがこのスライステストのコンポーネントスキャン対象に含まれる場合は前提が変わる可能性があるため、その時点で再確認する。
- 2026-07-08: `account-role.md`（UC-A1）を APP-ADR-0014 に合わせて更新完了（`api-designer`）。JWTレスポンスフィールド（`accessToken`）追加、認証・認可節の3段構成明記、参照ADRリストへのAPP-ADR-0014追加。
- 2026-07-08: `test-scenario-planner` によるUC-A1テストシナリオ設計完了。実装着手前の確認事項3点を決定: ①`AccountStatus.canLogin()` はUC-A1仕様（provisional/active→true、suspended/deactivated→false）に合わせて修正する（現状本番コード未使用のため影響なし）。②`GoogleSsoCallbackUseCase` はexec-planの記述通りGoogle検証→JIT→JWT発行の3段構成を統合する責務に変更する（Controller層での拒否判定は廃止）。③401/422用の新規例外クラスは提案通り `GoogleIdTokenVerificationException`（401）/ `InvalidIdTokenFormatException`（422）/ `DomainNotAllowedException`（422）を新設し、403は既存 `ForbiddenOperationException` を再利用する。

## 残課題・引き継ぎ事項
