# Agents ディレクトリ概要（人間向け）

このディレクトリ配下と、リポジトリルートに追加した設定によって「今できるようになったこと」と「使い方」をまとめる。

## 追加したファイルと役割

| ファイル | 役割 | 誰が読むか |
|------|------|------|
| [`.claude/settings.json`](../../.claude/settings.json) | ClaudeCodeの権限設定。`git push`、`rm`、`find -delete`、`rsync --delete`、`curl`/`wget` 等を禁止。`git commit` はdenyリストに含めず、CLAUDE.mdの「commit運用」に従い都度確認のうえ実行可 | ClaudeCode（自動適用） |
| [`CLAUDE.md`](../../CLAUDE.md) | ClaudeCodeの行動指針（禁止事項・推奨事項） | ClaudeCode（自動で読み込まれる） |
| [`AGENTS.md`](../../AGENTS.md) | リポジトリ全体の「目次」。docsの構成・サブエージェント一覧へのリンク集 | AIエージェント全般（自動で読み込まれる） |
| [`.claude/agents/doc-maintainer.md`](../../.claude/agents/doc-maintainer.md) | ドキュメント整備サブエージェント定義 | ClaudeCode（呼び出すと動く） |
| [`docs/exec-plans/`](../exec-plans/README.md) | 実行計画（進行中/完了/技術的負債）の運用ルールと実体 | 人間（あなた）・Claude |
| [`docs/design-docs/core-beliefs.md`](../design-docs/core-beliefs.md) | このリポジトリの運用原則・思想 | 人間（あなた）・Claude |
| [`openAI_harness_enjineerring.md`](openAI_harness_enjineerring.md) | 参考にした元記事の転記 | 人間（あなた）・Claude |

---

## 今できるようになったこと

### 1. 危険な操作の自動ブロック

ClaudeCodeに何を依頼しても、以下は実行されない（`.claude/settings.json` で拒否）。

- `git push`
- `rm` / `rmdir` / `find -delete` / `rsync --delete`
- `curl` / `wget` などインターネットからのダウンロード

→ **特別な操作は不要**。普段通り依頼するだけで、これらは自動的に弾かれる。
`git add` は常に許可されている。`git commit` はdenyリストでは禁止せず、コミットメッセージと `git diff --cached` をClaudeCodeが提示し、ユーザーの確認を得てから実行する（[CLAUDE.md](../../CLAUDE.md) の「commit運用」参照）。`git push` は人間が行う。

### 2. リポジトリの「地図」が常に読み込まれる

`AGENTS.md` はClaudeCode（および他のAIエージェント）が作業開始時に自動で参照する想定のファイル。
「このリポジトリのドキュメントはどこにあるか」「どんなサブエージェントが使えるか」を毎回説明し直す必要がなくなる。

→ **使い方**: 今後、新しいルールや知識を追加したい場合は、`AGENTS.md` に1行リンクを追加し、本文は `docs/` 配下に書く（`AGENTS.md` 自体は肥大化させない）。

### 3. ドキュメント整備サブエージェント（doc-maintainer）

`docs/` の索引漏れ・リンク切れ・ADR命名規則違反などをチェックする読み取り専用エージェント。

→ **使い方**: ClaudeCodeに次のように依頼する。

```
doc-maintainerサブエージェントでdocs/の整合性をチェックして
```

ファイルの作成・編集・削除は行わず、レポート（OK / 要対応リスト）のみを返す。
新しいドキュメントを追加した後や、`docs/` の構成を変えた後に使うと効果的。

### 4. 実行計画（exec-plans）と運用原則（design-docs）

- 複数PR・複数セッションに渡る作業は [docs/exec-plans/active/](../exec-plans/README.md) に記録され、進捗・意思決定ログが追記されていく。
- このリポジトリで大事にしている考え方は [docs/design-docs/core-beliefs.md](../design-docs/core-beliefs.md) にまとまっている。
- 1PR・1セッションで完結する小さな作業はexec-planファイルを作らず、TodoWriteのみで管理する。どちらにするかはClaudeCodeが提案し、人間が確認する。

---

### 5. ナビゲーション指標ログ（navigation-metrics.md）

[`navigation-metrics.md`](navigation-metrics.md) は、「AGENTS.mdを目次として使う」方針が
実際に機能しているかを、ClaudeCodeがセッション終了時に自己評価して記録するログ。
難易度・探索コストの2軸を1行追記する簡易フォーマットで、探索コストが続けて高い場合は
`doc-maintainer`が目次構成の見直し案を提示する仕組みになっている。

→ **使い方**: 特別な操作は不要。区切りの良いタイミングでClaudeCodeが自動的に追記する。
ログを見て「最近探索コストが高いセッションが多いな」と感じたら、
`doc-maintainerサブエージェントでナビゲーション指標をチェックして`と依頼すると整理案が出る。

### 6. 会話圧縮後もAGENTS.mdの内容を保持

長いセッションでコンテキストが圧縮（要約）されても、`CLAUDE.md`の内容（CLAUDE.mdは予約ファイルとして
常時自動読込される）と、そこから`@AGENTS.md`構文でimportされている`AGENTS.md`の全文は、
圧縮後も引き続きコンテキストに残ることを確認済み。

→ **使い方**: 特別な操作は不要。`CLAUDE.md`冒頭の`@AGENTS.md`が常時importを担う。

## 今後の流れ（Phase 2）

要件定義用のマルチエージェント構成（コンテキスト収集・ドメイン分析・要件ドラフト・レビューの各サブエージェント）は未着手。
詳細手順は [exec-plans/active/0001-requirements-definition-multiagent.md](../exec-plans/active/0001-requirements-definition-multiagent.md) を参照。

次のステップ: 構築したいシステムのコンテキスト（目的・ドメイン・制約）をClaudeCodeに共有する。
