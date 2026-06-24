# 0007: 認可・アクセス制御（provisional ガード・admin ロール）

## 完了条件（Definition of Done）

- `provisional` アカウントが保護エンドポイントに到達できない
- `admin` ロールが必要なエンドポイントで権限なしアクセスが 403 を返す
- Controller 引数がドメインモデルのみで Web 概念が UseCase 層に漏れていない

## 目的・スコープ

exec-plan 0006（認証基盤）完了後に着手。Spring Security の認可レイヤーと Controller 引数の共通化を実装する。

## 進捗状況

- [ ] ADR 作成: `provisional` 状態アクセス制御の実装レイヤー決定（Spring Security フィルタ vs `@PreAuthorize`）（`adr-governance` 経由）
- [ ] `HandlerMethodArgumentResolver` で `@AuthenticatedAccountId` 等のアノテーション実装
- [ ] Controller 引数をドメインモデルのみに保つ（UseCase 層に Web 概念を持ち込まない）
- [ ] テスト: `provisional` → 403、`active` → 200、`admin` ロール不足 → 403
- [ ] コードレビュー（`code-reviewer` → `test-reviewer`）
- [ ] PR 作成・マージ

> 根拠: [APP-ADR-0008](../../adr/APP-ADR-0008-DDD-CQRSアーキテクチャ原則の採用.md)

## 意思決定ログ

## 残課題・引き継ぎ事項
