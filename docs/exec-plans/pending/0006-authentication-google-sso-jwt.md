# 0006: 認証基盤（Google SSO + JWT）

## 完了条件（Definition of Done）

- `POST /api/auth/google/callback`（UC-A1）が実装され、Google ID トークン検証・JIT プロビジョニングが動作する
- `SecurityContextHolder` から `accountId` を取得できる仕組みが確定している

## 目的・スコープ

Spring Security で Google SSO 認証を実装する。認可（exec-plan 0007）の基盤となるため先行する。

## 進捗状況

- [ ] ADR 作成: JWT 戦略（自前発行 vs Google id_token Bearer）の決定（`adr-governance` 経由）
- [ ] `spring-boot-starter-oauth2-resource-server` またはカスタムフィルタの設計
- [ ] `GoogleSsoCallbackUseCase` の認証フロー実装（JIT プロビジョニング含む）
- [ ] `SecurityContextHolder` から `accountId` を取得する仕組みの確定（`@AuthenticationPrincipal` 等）
- [ ] API 設計書: `POST /api/auth/google/callback`（`api-designer` → `test-scenario-planner` → `kotlin-implementer`）
- [ ] コードレビュー（`code-reviewer` → `test-reviewer`）
- [ ] PR 作成・マージ

## 意思決定ログ

## 残課題・引き継ぎ事項
