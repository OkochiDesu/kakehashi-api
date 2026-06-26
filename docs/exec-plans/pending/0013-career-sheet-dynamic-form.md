# 0013: キャリアシート・ダイナミックフォーム基盤（Step2 開始）

## 完了条件（Definition of Done）

- キャリアシートのレイアウト定義（JSON マスタ）と入力データ（JSONB）が CRUD できる
- フォーマットバージョン移行ポリシーの ADR が作成済み

## 目的・スコープ

Step2 の最初の機能。exec-plan 0012（Step1 完了ゲート）後に着手する。
`data-models.md` には具体的なテーブル構造が未記載のため、ADR・詳細設計フェーズを最初に置く。

> このexec-planの詳細タスクは、Step2設計が固まった時点で更新する。

## 進捗状況

- [ ] バージョン移行ポリシーの ADR 作成（`adr-governance` 経由）
- [ ] DB 設計: `career_sheet_formats`（レイアウト定義）・`career_sheets`（JSONB 入力データ）（`db-designer`）
- [ ] Flyway マイグレーション
- [ ] API 設計書（`api-designer`）
- [ ] テストシナリオ設計（`test-scenario-planner`）
- [ ] Kotlin 実装（`kotlin-implementer`）
- [ ] コードレビュー（`code-reviewer` → `test-reviewer`）
- [ ] PR 作成・マージ

## 意思決定ログ

## 残課題・引き継ぎ事項

- フロントエンド（Metadata-Driven UI）との連携設計は別途
