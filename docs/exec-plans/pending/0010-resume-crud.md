# 0010: 経歴書コンテキスト CRUD（UC-R1〜R4）

## 完了条件（Definition of Done）

- `resumes` / `resume_qualifications` / `resume_projects` / `resume_project_skills` テーブルが Flyway マイグレーション済み
- UC-R1〜R4 の REST API が実装・テスト済み
- 経歴書保存時の星取表自動連携（`user_skills` UPSERT）が動作する

## 目的・スコープ

`data-models.md` 4章の経歴書コンテキストを実装する。`resume_project_skills` が `skill_master_items` を参照するため、exec-plan 0009（星取表 CRUD）完了後に着手する。

## 進捗状況

- [ ] DB 設計・Flyway マイグレーション（`db-designer` 経由）
  - `resumes` / `resume_qualifications` / `resume_projects` / `resume_project_skills`
- [ ] API 設計書: UC-R1〜R4（`api-designer` 経由）
- [ ] テストシナリオ設計（`test-scenario-planner` 経由）
- [ ] Kotlin 実装（`kotlin-implementer` 経由）
- [ ] 経歴書保存→星取表自動連携（`resume_project_skills` → `user_skills` UPSERT）の実装
- [ ] コードレビュー（`code-reviewer` → `test-reviewer`）
- [ ] PR 作成・マージ

> 根拠: [data-models.md 4章](../../requirements/data-models.md#4-経歴書コンテキスト) / [APP-ADR-0002](../../adr/APP-ADR-0002-星取表マスタと経歴書のデータ連携方針.md) / [APP-ADR-0003](../../adr/APP-ADR-0003-経歴書のマスク範囲-コンタクト経路-ファイル出力範囲のスコープ判断.md)

## 意思決定ログ

## 残課題・引き継ぎ事項

- 一括登録系（経歴書 CSV）は SQL 直接投入で対応（API 化しない）
