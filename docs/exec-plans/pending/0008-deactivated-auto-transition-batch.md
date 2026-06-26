# 0008: deactivated 自動遷移バッチ（`@Scheduled`）

## 完了条件（Definition of Done）

- `suspended_at` から1年経過したアカウントが日次で `deactivated` に更新される
- `@ConditionalOnProperty` でテスト時に意図せず動作しない

## 目的・スコープ

APP-ADR-0006 に基づく日次バッチ処理。他フェーズと独立して実装可能。

## 進捗状況

- [ ] `AccountDeactivationBatch.kt` の実装（`@Scheduled` 日次）
- [ ] `@ConditionalOnProperty` でテスト時無効化
- [ ] テスト: 1年経過アカウントが `deactivated` に変わること（`@TestPropertySource` で有効化して検証）
- [ ] コードレビュー（`code-reviewer` → `test-reviewer`）
- [ ] PR 作成・マージ

> 根拠: [APP-ADR-0006](../../adr/APP-ADR-0006-accounts.statusに4値設計（deactivated追加）と非adminからのsuspended-deactivated除外.md)

## 意思決定ログ

## 残課題・引き継ぎ事項
