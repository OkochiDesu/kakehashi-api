# 0021: ハーネス・ガードレール見直しとコーディングルール集約

## 完了条件（Definition of Done）

- ハーネス・ガードレール（`.claude/agents`・`.claude/rules`・`.claude/hooks`・`.claude/skills`・CLAUDE.md/AGENTS.md・docs/配下の運用ドキュメント）の全体チェックが完了し、指摘事項が棚卸しされている
- 棚卸しで洗い出された改善項目に順次対応済み。少なくとも以下を含む:
  - 実装・レビューエージェント（`kotlin-implementer`/`code-reviewer`等）が参照する実装ルール（KDoc規約・エラーメッセージ規約・命名規則等）が単一のコーディングルールドキュメントに集約され、各エージェント定義は参照のみで実体を持たない
- 対応結果が AGENTS.md / docs/agents/README.md 等の索引に反映済み
- PR 作成・マージ済み

## 目的・スコープ

開発が進み `.claude/agents`・`.claude/rules`・docs/ 配下の md ファイルの情報量が増えてきたため、AIエージェントが働きやすい環境（ハーネス・ガードレール）になっているかを一度全体点検する。点検で見つかった改善項目に順次対応し、その中に「実装で使うコーディングルールをエージェントごとに重複させず単一ドキュメントに集約する」という具体的な改善を含める。

この集約の必要性は、exec-plan 0006 の PR #21 対応中に実際に発生した事象（override メソッドの KDoc 省略例外というルール1つを `docs/conventions/kdoc-and-test-policy.md`・`.claude/agents/kotlin-implementer.md`・`.claude/agents/code-reviewer.md` の3ファイルへ同時に反映する必要が生じた）で具体的に確認済みの課題である。

exec-plan 0020（Account エンティティ・Repository リファクタリング）完了後に着手する。

## 進捗状況

### ① 全体チェック（監査フェーズ）

- [ ] `doc-maintainer-structure` / `doc-maintainer-content` を全体スコープ（diffスコープではなくリポジトリ全体）で実行し、陳腐化・不整合・索引漏れを洗い出す
- [ ] `.claude/agents/` 配下の各エージェント定義に、実装ルール（KDoc・エラーメッセージ・命名規則等）の重複記載がないか棚卸しする（`kotlin-implementer.md` / `code-reviewer.md` / `test-scenario-planner.md` 等）
- [ ] `docs/agents/navigation-metrics.md` のナビゲーション指標を確認し、閾値超過があれば対応要否を判断する
- [ ] 監査結果を本 exec-plan の「意思決定ログ」に一覧化し、対応する/しない項目をユーザーと合意する

### ② コーディングルール集約（プラン2）

- [ ] 実装ルールの集約先ドキュメントを決定する（新規作成 or 既存 `docs/conventions/kdoc-and-test-policy.md` の拡張、どちらが良いかユーザーと相談）
- [ ] `kotlin-implementer.md` / `code-reviewer.md` 等に重複記載されている実装ルールを集約先への参照に置き換える
- [ ] `mybatis-rules.md` / `test-rules.md` 等、既存 glob ルールとの役割分担を整理する
- [ ] doc-maintainer チェックで重複・矛盾が解消されたことを確認する

### ③ その他監査で見つかった改善項目

- [ ] （①の監査結果に応じて具体化する）

### ④ 仕上げ

- [ ] AGENTS.md / docs/agents/README.md 等の索引更新
- [ ] doc-maintainer チェック実施
- [ ] PR 作成・マージ

## 意思決定ログ

- 2026-07-19: ユーザーから「①ハーネス全体チェック→②その中でコーディングルール集約（プラン2）を含め順次対応」という進め方の合意を得て、1つの exec-plan として起票した。exec-plan 0020 完了後に着手する順序で合意した。
- 2026-07-19: exec-plan 0020（PR #23）のレビュー対応完了に伴い、`pending/`から`active/`へ移動し着手可能な状態にした。exec-plan 0020対応中に、KDoc規約（`@property`タグ・非自明なoverrideの説明）を`kdoc-and-test-policy.md`・`kotlin-implementer.md`・`code-reviewer.md`の3ファイルへ同時反映する事象が再度発生しており（②コーディングルール集約の必要性を裏付ける追加事例）、①監査フェーズで参照すること。

## 残課題・引き継ぎ事項

- なし（起票時点）
