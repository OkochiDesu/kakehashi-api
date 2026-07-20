# 0007: 認可・アクセス制御（provisional ガード・admin ロール）

## 完了条件（Definition of Done）

- `provisional` アカウントが保護エンドポイントに到達できない
- `admin` ロールが必要なエンドポイントで権限なしアクセスが 403 を返す
- Controller 引数がドメインモデルのみで Web 概念が UseCase 層に漏れていない
- JWT の principal（accountId）と `X-Account-Id`/`X-Is-Admin` ヘッダーの不整合によるなりすましができない
  （ヘッダー方式が廃止されている、または JWT principal との突合で不一致を 401/403 にしている）

## 目的・スコープ

exec-plan 0006（認証基盤）完了後に着手。Spring Security の認可レイヤーと Controller 引数の共通化を実装する。

## 進捗状況

- [ ] ADR 作成: `provisional` 状態アクセス制御の実装レイヤー決定（Spring Security フィルタ vs `@PreAuthorize`）（`adr-governance` 経由）
- [ ] `AccountController` の `X-Account-Id`/`X-Is-Admin` ヘッダー方式を廃止し、`SecurityContextHolder`
  （[JwtAuthenticationFilter](../../../src/main/kotlin/com/kakehashi/infrastructure/account/JwtAuthenticationFilter.kt)
  がセットする JWT principal）ベースの本人特定・権限判定に置き換える
- [ ] `HandlerMethodArgumentResolver` で `@AuthenticatedAccountId` 等のアノテーション実装
- [ ] Controller 引数をドメインモデルのみに保つ（UseCase 層に Web 概念を持ち込まない）
- [ ] テスト: `provisional` → 403、`active` → 200、`admin` ロール不足 → 403
- [ ] コードレビュー（`code-reviewer` → `test-reviewer`）
- [ ] PR 作成・マージ

> 根拠: [APP-ADR-0008](../../adr/APP-ADR-0008-DDD-CQRSアーキテクチャ原則の採用.md)

## 意思決定ログ

- 2026-07-18: PR #21（exec-plan 0006）の Copilot レビューで、`SecurityConfig` に
  `anyRequest().authenticated()` を導入した結果、有効な JWT を持つ任意の認証済みユーザーが
  `X-Account-Id`/`X-Is-Admin` ヘッダーを偽装して他人の accountId・admin 権限を名乗れる
  （JWT principal との突合がない）指摘を受けた。exec-plan 0006 の既存スコープ境界
  （認可＝ロールベースの `@PreAuthorize` 等は exec-plan 0007 のスコープ）を尊重し、
  本 PR では修正せず既知の残課題として本 exec-plan の DoD・タスクに反映する方針とした。

## 残課題・引き継ぎ事項
