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
- `git commit` の前に、現在のブランチに対応するPRの状態を `gh pr view --json state,number` で確認する
  - PRがマージ済み（`state: MERGED`）の場合、現在のブランチでの作業は完了済みとみなし、新しい作業ブランチを作成・切り替え（`git checkout -b feature/<内容> origin/main`）してからcommitを進める
  - 新しいブランチ名は、作業内容をユーザーに確認したうえで決定する（[Git Prefixes](docs/conventions/git-prefixes.md)のブランチ名規約に従い `feature/` プレフィックスを付ける）
  - PRが存在しない、または未マージ（`OPEN`等）の場合は、現在のブランチのままcommitを進める
  - 上記のPRマージ済みチェックは `.githooks/pre-commit` でも自動実行され、MERGEDの場合はcommit自体がブロックされる（[pre-commit-secret-check.md](docs/conventions/pre-commit-secret-check.md)）。ただしブランチの作成・切り替えはフックでは行わないため、上記の手順に従って対応する
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
