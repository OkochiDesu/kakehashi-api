# CLAUDE.md

このファイルはClaudeCodeがこのリポジトリで作業する際の行動指針を定義する。
リポジトリ全体のドキュメント構成・サブエージェントの一覧は [AGENTS.md](AGENTS.md) を参照すること（以下でimportし常時読込）。

@AGENTS.md

## 作業ルール

### コミュニケーション
- ユーザーへの応答は日本語で行う

### 次の作業が決まったときの exec-plan 作成
- ユーザーが「次は〇〇をやりたい」「〇〇を差し込みたい」など次の作業への意思を示した場合、その場で [exec-plan の3条件](.claude/rules/exec-plan-rules.md#todo--exec-plan-昇格基準)（DoD が書ける・主要タスク3件以上・PR目的が明確）を満たすか判定し、満たせばすぐ exec-plan を作成する
  - **TODO.md への追記だけで終わらない**。TODO.md はアイデア・未具体化の段階。着手意思がある = exec-plan が必要
  - セッションをまたいでも迷子にならないよう、「今から何をするか」を exec-plan に記録してから着手する

### タスク開始時のゴール定義とプラン作成
- **3ステップ以上 または 複数ファイルにまたがるタスク**は、作業開始前に以下の順序で進める:
  1. **ゴール定義（完了条件）**: 「このタスクが完了したとき、〇〇ができる／〇〇の状態になっている」を1〜3文で定義する。ユーザーへの提示は不要だが、自分の判断基準として明文化すること
  2. **プラン作成**: `TodoWrite` でタスクを分解する
     - **見える範囲**: 直接変更するファイル・操作
     - **見えない範囲**: 変更によって波及する可能性のある範囲（索引更新・ADR・doc-maintainer チェック等）
- プランの作成にユーザー確認は不要。作業しながら完了したものを順次チェックする
- 単純な1〜2ステップのタスク（1ファイルの修正・質問への回答等）はゴール定義・プラン不要

### 禁止事項
- `gh pr merge` は実行しない（GitHub PR のマージは人間が行う）
- `git reset --hard`, `git clean` など破壊的なgit操作は実行しない
- `rm`, `rmdir` など削除コマンドは使用しない
- `find -delete` や `rsync --delete` など再帰的な削除操作は使用しない
- `curl`, `wget` などでインターネットからファイルをダウンロードしない
- 外部URLへのアクセス（内容の読み取り）は、人間の許可を得てから行うこと
- `.claude/settings.json` を編集した後は必ず `jq . .claude/settings.json` で JSON 構文を確認すること（フォーマット不正によるパースエラー防止のため）
- `.claude/settings.json` の deny リストを変更した場合は、`docs/agents/README.md`・`docs/design-docs/core-beliefs.md`・`harness-and-guardrails.md` の説明も同一コミットで更新すること
- CLAUDE.md / AGENTS.md にスキル・コマンド・ファイルパスを記述する前に、対象が実在するかを確認すること（実在しないリソースへの参照は実行不能なルールになる）

### commit運用
- **`main` ブランチへの直接 commit は禁止**。chore・metrics 記録・typo 修正など小さな変更も例外なく `feature/` ブランチを作成してから commit すること
- `git commit` の前に、現在のブランチに対応するPRの状態を `gh pr view --json state,number` で確認する
  - PRがマージ済み（`state: MERGED`）の場合、現在のブランチでの作業は完了済みとみなし、新しい作業ブランチを作成・切り替え（`git checkout -b feature/<内容> origin/main`）してからcommitを進める
  - 新しいブランチ名は、作業内容をユーザーに確認したうえで決定する（[Git Prefixes](docs/conventions/git-prefixes.md)のブランチ名規約に従い `feature/` プレフィックスを付ける）
  - PRが存在しない、または未マージ（`OPEN`等）の場合は、現在のブランチのままcommitを進める
  - 上記のPRマージ済みチェックは `.githooks/pre-commit` でも自動実行され、MERGEDの場合はcommit自体がブロックされる（[pre-commit-secret-check.md](docs/conventions/pre-commit-secret-check.md)）。ただしブランチの作成・切り替えはフックでは行わないため、上記の手順に従って対応する
- **doc-maintainer チェック（必須）**: `docs/` 配下のファイルを含む変更をコミットする前に陳腐化チェックを行うこと。チェックは2つのエージェントに分割されており、状況に応じて使い分ける
  - **コミット前（軽量・必須）**: `doc-maintainer-structure` サブエージェントを diff スコープで呼び出し、索引・リンク整合性・`.claude/` 構成・ToC を確認する
  - **新規ファイル追加を含むコミット前（必須）**: `doc-maintainer-structure` と `doc-maintainer-content` を**並列で**呼び出す。新規ファイルは diff スコープでは「既存ドキュメントからの言及漏れ」や「既存 ADR との矛盾」を検出できないため
  - **定期チェック（PR作成前 / ユーザー明示指示時）**: `doc-maintainer-structure` と `doc-maintainer-content` を**並列で**呼び出し、全体の陳腐化・ADR整合・exec-plans・TODO実行可能性を確認する
  - チェック範囲は diff に含まれるファイルに関連する範囲に絞る（コミット前チェックの場合）
  - 実装ファイルの変更であっても、関連する設計書・ADR・`docs/` 側の記述が陳腐化していないかを確認すること
  - チェック結果に要対応事項がある場合は、修正を同一コミットに含めてから進める
- `git commit` は、コミットメッセージと `git diff --cached` の内容をユーザーに提示し、明示的な確認を得た場合にのみ実行する（**Auto Mode がアクティブな場合も例外なし**。「タスクへの承認」はコミットへの承認ではない）
- 変更が複数の関心事にまたがる場合は、意味のある単位で複数のcommitに分けることを提案する
- commitのauthor/committerはユーザーのgit configに紐づくため、誰が確認・実行したかはcommitメタデータに残る
- `git push` は、プッシュ先リモートブランチ・対象コミット一覧をユーザーに提示し、明示的な確認を得てから実行する（**Auto Mode がアクティブな場合も例外なし**）
- `git merge` は、マージ元ブランチ・影響範囲をユーザーに提示し、明示的な確認を得てから実行する（**Auto Mode がアクティブな場合も例外なし**）
- **PR作成時**は `.github/pull_request_template.md` のセクション構成に従うこと。各セクションの扱いは以下の通り:
  - `# 概要`: 必須。変更内容を簡潔に記載する
  - `## 変更内容`: `<!-- pr-summary:start -->` / `<!-- pr-summary:end -->` マーカーを必ず残す。CI が自動生成するため手動で変更しない
  - `## 動作確認（自動）`: チェックボックス「CI が通ること」をそのまま残す（記述不要）
  - `## 動作確認（手動）`: 任意。ソースコードの変更を含む場合のみ記載する。不要なら省略してよい
  - `## 気になる部分` / `## 補足`: 任意。内容がある場合のみ記載する。不要なら省略してよい
- **PR指摘への返信**は、内容をユーザーに提示し確認を得てから投稿すること（Auto Mode がアクティブな場合も例外なし）
  - **指摘への対応（コード修正・ドキュメント更新）が完了したら、同一セッション内で返信投稿まで完結させること**。修正で止まり返信を忘れる漏れが起きやすいため、対応完了 ≠ 返信完了と意識すること
  - 先送りにする場合は「理由（範囲外・別ブランチ等）」と「対応タイミング（ブランチ名 / 時期 / 未定）」を必ず明記すること
  - 返信にはコミットリンクを付与すること。同一リポジトリの場合は短縮 SHA（7文字）のみでよい（GitHub が自動リンクするため `リポジトリ名@` プレフィックスは不要）
  - SHA を文中に埋め込む場合は SHA の前に半角スペース1つ、後に半角スペース2つを入れること（GitHub の自動リンクが有効になる）（例: `対応済みです。 a1b2c3d  で修正しています。`）

### CIフック・ワークフロー変更時の確認（必須）
- `.githooks/` 配下のフックファイルおよび `.github/workflows/` 配下のワークフローファイルを変更する場合は、**変更内容の分析・分類結果をユーザーに提示し、確認を得てから実装すること**
- 理由: CIの制限はチーム全体に影響し、除外パターンの妥当性はユーザーが判断すべき意思決定であるため
- **シェルスクリプト内のコメントは `# shellcheck` で始めてはならない**（shellcheck がディレクティブとして誤パースし SC1072/SC1073 エラーが発生するため）。`# shellcheck disable=SCXXXX` など正規のディレクティブ以外は別の書き出しにすること
- **シェルスクリプトで文字クラス・範囲を使う場合は `LC_ALL=C` を前置すること**（例: `LC_ALL=C grep -v '[^ -~]'`）。ロケール設定によって `\x80-\xFF` や `[A-Za-z]` の挙動が変わるため、C ロケールで ASCII 範囲を明示することで環境依存を回避する

### 推奨事項
- ファイルを変更する前に、必ず現在の内容を読み取ること
- 変更は最小限にとどめ、副作用のある操作（破壊的・不可逆な操作）は事前にユーザーに確認すること
- 不明な点や判断が必要な場合は、ユーザーに質問すること
- 確認事項をユーザーに質問する前に、既存のADR（[docs/adr/](docs/adr/)）と要件定義ドキュメント（[docs/requirements/](docs/requirements/)）を確認し、既に決定済みの内容を除外すること
- 意思決定が行われた場合（代替案があり、選んだ理由が現在の文脈に依存し、将来変わりうるもの）は、ADR または AI-ADR の作成をユーザーに提案すること
- ユーザーから指摘・訂正を受けた場合は、以下の分類で仕組み化を提案すること（[core-beliefs.md 原則7](docs/design-docs/core-beliefs.md) 参照）:
  - 一時的な文脈ミス → `memory/` に保存
  - プロジェクト固有の行動ルール違反 → `CLAUDE.md` にルール追記
  - 特定エージェントの設計ミス → `.claude/agents/` を更新
  - 設計判断レベルの問題 → AI-ADR として記録
  - **上記に加え、grep/正規表現で確実に検出できるパターンかを判断すること**（YES → `.githooks/pre-commit` にも追加。上記分類と組み合わせ可。判断基準は [harness-and-guardrails.md](docs/design-docs/harness-and-guardrails.md) 参照）
- ADR（`{PREFIX}-ADR-XXXX-`、PREFIX: `APP` / `CICD` / `DOC`）または AI-ADR（`AI-ADR-XXXX-`）を作成・更新・Supersede する際は、`adr-governance` サブエージェントを呼び出すこと（ユーザーが `/adr-governance` スキルを起動した場合はスキル側が呼び出すため不要）
- **ADR の `影響` 欄に実装方針（認可ロジック・楽観ロック・パス形式等）が記載されている場合、`.claude/agents/` 配下の関連エージェント定義（特に `kotlin-implementer.md` / `code-reviewer.md` / `api-designer.md`）が陳腐化していないか合わせて確認し、必要なら更新すること**
- **バージョン文字列（Dockerイメージタグ・ライブラリバージョン等）をコード・設定で変更した場合は、`grep -r` で `docs/` および `.claude/rules/` の同一文字列を検索し、陳腐化した参照を一括更新すること**（例: テストコンテナのイメージタグを変更したら troubleshooting ドキュメントやテスト規約サンプルも更新）

## 関連ドキュメント
- 全体マップ: [AGENTS.md](AGENTS.md)
- 運用原則: [docs/design-docs/core-beliefs.md](docs/design-docs/core-beliefs.md)
- 実行計画（exec-plans）の運用ルール: [docs/exec-plans/README.md](docs/exec-plans/README.md)
- マルチエージェント構成の詳細: [docs/exec-plans/completed/0001-requirements-definition-multiagent.md](docs/exec-plans/completed/0001-requirements-definition-multiagent.md)
- ドキュメント整備サブエージェント（分割型）: [doc-maintainer-structure](.claude/agents/doc-maintainer-structure.md) / [doc-maintainer-content](.claude/agents/doc-maintainer-content.md)
