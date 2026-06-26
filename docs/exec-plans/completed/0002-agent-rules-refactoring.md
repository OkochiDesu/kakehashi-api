# 0002: エージェント・ルール構成のリファクタリング

## 目的・スコープ

`code-reviewer.md` / `kotlin-implementer.md` に蓄積したチェック項目を
「スクリプト・glob ルール・専用エージェント」に分離し、各ファイルを小さく保つ。

背景:
- テスト観点・MyBatis 観点・スクリプト化可能な観点が `code-reviewer.md` に混在してきた
- LLM がやるべき仕事（意味的チェック）とスクリプトがやるべき仕事（パターン検出）を分離する
- `test-reviewer` を追加し、`code-reviewer` APPROVED 後に自動呼び出しすることでレビューの粒度を上げる

## 進捗状況

### Phase 1: pre-commit スクリプト強化
- [x] 英語エラーメッセージ検出を `.githooks/pre-commit` に追加
- [x] `assert(` 誤使用検出を `.githooks/pre-commit` に追加

### Phase 2: `.claude/rules/` glob ルール作成
- [x] `.claude/rules/test-rules.md`（globs: `**/*Test.kt`）作成
- [x] `.claude/rules/mybatis-rules.md`（globs: `**/*Mapper.xml`, `**/*Mapper.kt`）作成

### Phase 3: `test-reviewer.md` エージェント作成
- [x] `.claude/agents/test-reviewer.md` 作成
- [x] `implement-review-loop` スキルに `test-reviewer` 呼び出しを追加（code-reviewer APPROVED 後）

### Phase 4: `code-reviewer.md` 軽量化
- [x] 移管済み観点（テスト・MyBatis）を削除し cross-reference に置換

### Phase 5: `kotlin-implementer.md` 軽量化
- [x] 移管済み観点（MyBatis・テスト手順）を削除し cross-reference に置換

### Phase 6: 索引・ドキュメント更新
- [x] `AGENTS.md` に `test-reviewer` と `.claude/rules/` を追記
- [x] `docs/agents/README.md` に `test-reviewer`・glob ルールを追記
- [x] `docs/exec-plans/README.md` の「次に使用できる番号」を 0003 に更新
- [x] `docs/README.md` 更新（harness-and-guardrails.md のリンクを追記済み）

### Phase 7: 旧重複コンテンツのクリーンアップ（ユーザー確認後）
- [x] `code-reviewer.md` から APP-ADR-0005 のテスト検証記述を削除（test-reviewer に委譲済みのため）
- [x] `kotlin-implementer.md` — 削除候補なし（Phase 5 で整理済み）
- [x] 不要になったファイル — 該当なし（Windows通知ファイルは前セッションでリバート済み）

## 意思決定ログ

- 2026-06-22: `test-reviewer` の呼び出しは code-reviewer APPROVED 後の順次実行とする（並列不採用の理由: 本体コードに REQUIRES_CHANGES が出た場合、テストも書き直しになることが多く並列実行はコール無駄になる）
- 2026-06-22: 旧ファイルは移行完了まで残し、Phase 7 でユーザーが確認後に削除する方針

## 残課題・引き継ぎ事項

- Controller 専用の glob ルール（`controller-rules.md`）は、Controller 観点のルールが十分蓄積してから追加を検討（現時点では `code-reviewer.md` に残す）
