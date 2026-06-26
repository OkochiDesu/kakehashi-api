# 0016: README 整理 + エージェント・ハーネス整理

## 完了条件（Definition of Done）

- 全 README（12件）の不要記述・陳腐化・ベース記載の過不足を解消済み
- `.claude/agents/`・`.claude/rules/`・`.claude/hooks/`・`.claude/skills/` の各定義が現状のコードベース・運用と一致
- PR 作成・マージ済み

## 目的・スコープ

リポジトリ全体の README とエージェント・ハーネス定義を棚卸しする。
README は学習を兼ねてユーザーと会話しながら進める（削除・変更・現状維持の3択）。
エージェント・ハーネスは陳腐化・不整合・不要定義を洗い出し整理する。

## 進捗状況

### README 整理（12件）

#### docs/ 系

- [ ] `docs/README.md` — ドキュメント索引
- [ ] `docs/adr/README.md` — ADR 索引・運用ルール
- [ ] `docs/agents/README.md` — マルチエージェント構成
- [ ] `docs/architecture/README.md` — モジュール構成・パッケージ設計
- [ ] `docs/database/README.md` — テーブルカタログ
- [ ] `docs/exec-plans/README.md` — exec-plans 運用ルール
- [ ] `docs/exec-plans/completed/README.md` — 完了済み exec-plans 索引
- [ ] `docs/references/README.md` — 外部参考資料索引
- [ ] `docs/requirements/README.md` — 要件定義索引
- [ ] `docs/requirements/inputs/miro/README.md` — Miro インポート素材

#### src/ 系（クラス図）

- [ ] `src/main/kotlin/com/kakehashi/domain/account/README.md`
- [ ] `src/main/kotlin/com/kakehashi/usecase/account/README.md`

### エージェント・ハーネス整理

- [ ] `.claude/agents/` — 各エージェント定義（陳腐化・不整合・不要定義の洗い出し）
- [ ] `.claude/rules/` — glob ルール（test-rules.md・mybatis-rules.md）
- [ ] `.claude/hooks/` — hook スクリプト（動作・説明の整合性確認）
- [ ] `.claude/skills/` — スキル定義（adr-governance・implement-review-loop）
- [ ] PR 作成・マージ

## 意思決定ログ

## 残課題・引き継ぎ事項
