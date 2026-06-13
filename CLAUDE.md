# CLAUDE.md

このファイルはClaudeCodeがこのリポジトリで作業する際の行動指針を定義する。
リポジトリ全体のドキュメント構成・サブエージェントの一覧は [AGENTS.md](AGENTS.md) を参照すること。

## 作業ルール

### 禁止事項
- `git push` は実行しない（pushは人間が行う）
- `rm`, `rmdir` など削除コマンドは使用しない
- `find -delete` や `rsync --delete` など再帰的な削除操作は使用しない
- `curl`, `wget` などでインターネットからファイルをダウンロードしない

### commit運用
- `git commit` は、コミットメッセージと `git diff --cached` の内容をユーザーに提示し、明示的な確認を得た場合にのみ実行する
- 変更が複数の関心事にまたがる場合は、意味のある単位で複数のcommitに分けることを提案する
- commitのauthor/committerはユーザーのgit configに紐づくため、誰が確認・実行したかはcommitメタデータに残る
- `git push` は行わない。pushは人間が行う

### 推奨事項
- ファイルを変更する前に、必ず現在の内容を読み取ること
- 変更は最小限にとどめ、副作用のある操作（破壊的・不可逆な操作）は事前にユーザーに確認すること
- 不明な点や判断が必要な場合は、ユーザーに質問すること

## 関連ドキュメント
- 全体マップ: [AGENTS.md](AGENTS.md)
- 運用原則: [docs/design-docs/core-beliefs.md](docs/design-docs/core-beliefs.md)
- 実行計画（exec-plans）の運用ルール: [docs/exec-plans/README.md](docs/exec-plans/README.md)
- マルチエージェント構成の詳細: [docs/exec-plans/active/0001-requirements-definition-multiagent.md](docs/exec-plans/active/0001-requirements-definition-multiagent.md)
- ドキュメント整備サブエージェント: [.claude/agents/doc-maintainer.md](.claude/agents/doc-maintainer.md)
