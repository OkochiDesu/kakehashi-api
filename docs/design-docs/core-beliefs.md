# core-beliefs（運用原則）

このリポジトリでAIエージェントと協働する際に大事にしている原則をまとめる。
「なぜこの構成にしたか」の判断基準として使う。新しいルールを追加する際は、まず既存の原則と矛盾しないか確認すること。

参考: [Harness Engineering（OpenAI記事）](../references/harness-engineering/openai-harness-engineering.md)

## 1. AGENTS.mdは目次（マップ）であり、百科事典ではない

- [AGENTS.md](../../AGENTS.md) には「どこに何があるか」のリンクのみを書く。
- ルールの本文・詳細は `docs/` 配下の各ドキュメントに書き、AGENTS.mdからリンクする。
- 理由: 1つの大きな指示ファイルはコンテキストを圧迫し、エージェントが重要な制約を見落とす原因になる（記事の「コンテキストは希少な資源」「過度なガイダンスは指針を失う」）。

## 2. 危険な操作は機械的に防ぎ、行動指針で明文化する（二重の防御）

- 機械的な防御: [.claude/settings.json](../../.claude/settings.json) の deny リストで `git push`、`rm`系、`find -delete`/`rsync --delete`、`curl`/`wget` をブロックする。
- 行動指針: [CLAUDE.md](../../CLAUDE.md) に同内容を明文化し、エージェントが意図を理解した上で同じ判断ができるようにする。
- `git commit` は、コミットメッセージと `git diff --cached` をユーザーに提示し、明示的な確認を得た場合にのみAIが実行できる（settings.jsonのdenyリストには含めず、確認プロンプトで都度承認する運用）。`git push` は人間が行う。
- commitのauthor/committerはユーザー自身のgit configに紐づき、AIの関与は `Co-Authored-By` で記録されるため、誰が承認・実行したかはcommitメタデータから追跡できる。
- 追加の防御として、`.githooks/pre-commit` でシークレットらしき文字列の簡易スキャンを行う（[docs/conventions/pre-commit-secret-check.md](../conventions/pre-commit-secret-check.md)）。

## 3. ドキュメントの整合性は読み取り専用エージェントがチェックする

- [doc-maintainer](../../.claude/agents/doc-maintainer.md) が `docs/` の索引網羅性・リンク整合性・鮮度をチェックする。
- ファイルの作成・編集・削除は行わず、レポートのみを返す。適用判断は人間（または呼び出し元）に委ねる。
- 新しいドキュメント追加後・docs構成変更後、および実装ファイルの変更時（関連する設計書・ADRの陳腐化確認）にコミット前に呼び出す。

## 4. ADRはリポジトリの事実のみを根拠に、証拠ベースで更新する

- [adr-governance](../../.claude/agents/adr-governance.md) が git diff と既存ADRから影響分析を行い、ユーザー確認後にADRを作成・更新・Supersedeする。
- 推測でADRを作らない。証拠が不十分な場合は処理を止めて質問する。

## 5. 計画は第一級の成果物として扱う（exec-plans）

- 複数PR・複数セッションに渡る作業は [docs/exec-plans/](../exec-plans/README.md) の `active/` に記録し、進捗・意思決定ログを更新しながら進める。
- 1PR・1セッションで完結する作業はTodoWriteのみで管理し、exec-planファイルは作らない。
- どちらで進めるかは、ClaudeCodeが判定結果を提案し、**人間が確認する**。

## 6. 1機能・1PR単位でのヒューマンインザループ

- このリポジトリでは、すべてをAIに任せきりにしない。1機能・1PR単位で人間の確認を挟む。
- exec-planの作成判断、ADRの編集適用、ドキュメント整備の結果反映など、最終判断は常に人間が行う。

## 7. ADRとexec-plan意思決定ログの使い分け

- **ADR（[docs/adr/](../adr/README.md)）**: kakehashi-api という**製品・システムそのもの**に関する恒久的な決定。アーキテクチャ・業務仕様・データモデル・CI/CDポリシー・セキュリティポリシーなど（ADR一覧のカテゴリ表を参照）。将来このシステムに触る誰もが「なぜこの仕組みか」を索引表から探せる前提で書く。
- **exec-plan意思決定ログ（[docs/exec-plans/active/](../exec-plans/README.md)）**: その実行計画を進める**過程**での意思決定。特にCLAUDE.md・`.claude/`・`.githooks/`・devcontainerなど、**AIエージェントとの協働方法・開発プロセス**に関する決定はここに記録する。
- 迷ったときの軸: 「この決定はkakehashi-api（製品）の振る舞い・仕様に関するものか」→ ADR。「この決定はこのリポジトリでの開発・AIエージェント運用の進め方に関するものか」→ exec-plan意思決定ログ。
- ADRの対象はCI/CDだけでなく、今後アーキテクチャ・業務仕様の決定も含める予定（[docs/adr/README.md](../adr/README.md)のカテゴリ表に既に列挙済み）。
