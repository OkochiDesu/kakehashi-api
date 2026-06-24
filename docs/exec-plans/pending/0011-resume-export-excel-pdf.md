# 0011: 経歴書帳票出力（Excel / PDF）（UC-F1）

## 完了条件（Definition of Done）

- エンジニアの経歴書を Excel ダウンロード・PDF ダウンロードできる
- UseCase 層は `SkillSheetExporter` インターフェースのみに依存している

## 目的・スコープ

`data-models.md` 5章（ファイルコンテキスト）。exec-plan 0010（経歴書 CRUD）完了後に着手する。
Excel を正本とし LibreOffice headless で PDF 変換する方式（TODO.md より）。

## 進捗状況

- [ ] devcontainer Dockerfile に `fonts-noto-cjk` + `libreoffice` を追加（ユーザー確認後）
- [ ] `SkillSheetExporter` インターフェース定義（UseCase 層）
- [ ] `Jxls-poi` 導入・Excel テンプレート作成
- [ ] `JxlsSkillSheetExporter.kt` 実装（Infrastructure 層、`ProcessBuilder` で `soffice` 呼び出し）
- [ ] API 設計書: `GET /api/engineers/{id}/skill-sheet/excel` / `.../pdf`（`api-designer`）
- [ ] コードレビュー（`code-reviewer` → `test-reviewer`）
- [ ] PR 作成・マージ

## 意思決定ログ

## 残課題・引き継ぎ事項
