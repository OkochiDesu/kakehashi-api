# 0009: 星取表コンテキスト CRUD（UC-S1〜S5）

## 完了条件（Definition of Done）

- `skill_categories` / `skill_master_items` / `level_categories` / `level_master_items` / `user_skills` テーブルが Flyway マイグレーション済み
- UC-S1〜S5 の REST API が実装・テスト済み

## 目的・スコープ

`data-models.md` 3章の星取表コンテキストを実装する。`resume_project_skills` から `skill_master_items` を参照するため、経歴書 CRUD（exec-plan 0010）より先行する。

## 進捗状況

- [ ] DB 設計・Flyway マイグレーション（`db-designer` 経由）
  - `skill_categories` / `skill_master_items` / `level_categories` / `level_master_items` / `user_skills`
- [ ] API 設計書: UC-S1〜S5（`api-designer` 経由）
- [ ] テストシナリオ設計（`test-scenario-planner` 経由）
- [ ] Kotlin 実装（`kotlin-implementer` 経由）
- [ ] コードレビュー（`code-reviewer` → `test-reviewer`）
- [ ] PR 作成・マージ

> 根拠: [data-models.md 3章](../../requirements/data-models.md#3-星取表コンテキスト) / [APP-ADR-0002](../../adr/APP-ADR-0002-星取表マスタと経歴書のデータ連携方針.md)

## 意思決定ログ

## 残課題・引き継ぎ事項

- 一括登録系（星取表マスタ一括・CSV）は SQL 直接投入で対応（API 化しない）
