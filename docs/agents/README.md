# Agents ディレクトリ概要（人間向け）

このディレクトリ配下と、リポジトリルートに追加した設定によって「今できるようになったこと」と「使い方」をまとめる。

## 追加したファイルと役割

| ファイル | 役割 | 誰が読むか |
|------|------|------|
| [`.claude/settings.json`](../../.claude/settings.json) | ClaudeCodeの権限設定。`git push`、`rm`、`find -delete`、`rsync --delete`、`curl`/`wget` 等を禁止。`git commit` はdenyリストに含めず、CLAUDE.mdの「commit運用」に従い都度確認のうえ実行可 | ClaudeCode（自動適用） |
| [`CLAUDE.md`](../../CLAUDE.md) | ClaudeCodeの行動指針（禁止事項・推奨事項） | ClaudeCode（自動で読み込まれる） |
| [`AGENTS.md`](../../AGENTS.md) | リポジトリ全体の「目次」。docsの構成・サブエージェント一覧へのリンク集 | AIエージェント全般（自動で読み込まれる） |
| [`.claude/agents/doc-maintainer.md`](../../.claude/agents/doc-maintainer.md) | ドキュメント整備サブエージェント定義 | ClaudeCode（呼び出すと動く） |
| [`.claude/agents/adr-governance.md`](../../.claude/agents/adr-governance.md) | ADR作成・更新・Supersedeのオーケストレーター（model: opus） | ClaudeCode（呼び出すと動く） |
| [`.claude/agents/adr-search.md`](../../.claude/agents/adr-search.md) | ADR候補の検索（adr-governanceから呼び出し、model: haiku） | ClaudeCode（呼び出すと動く） |
| [`.claude/agents/adr-validator.md`](../../.claude/agents/adr-validator.md) | ADRドラフトのポリシー準拠検証（adr-governanceから呼び出し、model: haiku） | ClaudeCode（呼び出すと動く） |
| [`.claude/agents/db-designer.md`](../../.claude/agents/db-designer.md) | Flywayマイグレーション設計・作成エージェント | ClaudeCode（呼び出すと動く） |
| [`.claude/agents/api-designer.md`](../../.claude/agents/api-designer.md) | REST API設計書生成エージェント | ClaudeCode（呼び出すと動く） |
| [`.claude/agents/kotlin-implementer.md`](../../.claude/agents/kotlin-implementer.md) | Spring Boot (Kotlin) 実装エージェント | ClaudeCode（呼び出すと動く） |
| [`.claude/agents/code-reviewer.md`](../../.claude/agents/code-reviewer.md) | 実装コードレビューエージェント | ClaudeCode（呼び出すと動く） |
| [`.claude/skills/implement-review-loop/SKILL.md`](../../.claude/skills/implement-review-loop/SKILL.md) | `/implement-review-loop` スキル。kotlin-implementer→code-reviewerをAPPROVEDまでループ | ClaudeCode（`/implement-review-loop` で起動） |
| [`.claude/hooks/navigation-metrics-check.sh`](../../.claude/hooks/navigation-metrics-check.sh) | SessionStart時に navigation-metrics.md の閾値チェックを行い、超過時に警告をコンテキストへ注入 | ClaudeCode（自動実行） |
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
実際に機能しているかを、ClaudeCodeがPR作成後や作業区切りに自己評価して記録するログ。
難易度・探索コストの2軸を1行追記する簡易フォーマット。

実行タイミングは3種類ある:

| タイミング | 実行者 | 内容 |
|---|---|---|
| **セッション開始時（自動）** | SessionStart hook | 閾値超過を検知したら警告をコンテキストへ注入 |
| **手動分析** | doc-maintainer | 目次構成の具体的な見直し案を提示 |
| **記録** | ClaudeCode | PR作成後や作業区切りに難易度・探索コストを追記 |

→ **使い方**: 閾値超過の警告はセッション開始時に自動で通知される。詳しい分析が必要なら
`doc-maintainerサブエージェントでナビゲーション指標をチェックして`と依頼する。

### 6. 会話圧縮後もAGENTS.mdの内容を保持

長いセッションでコンテキストが圧縮（要約）されても、`CLAUDE.md`の内容（CLAUDE.mdは予約ファイルとして
常時自動読込される）と、そこから`@AGENTS.md`構文でimportされている`AGENTS.md`の全文は、
圧縮後も引き続きコンテキストに残ることを確認済み。

→ **使い方**: 特別な操作は不要。`CLAUDE.md`冒頭の`@AGENTS.md`が常時importを担う。

### 7. 実装・レビューループスキル（implement-review-loop）

`/implement-review-loop <UC名 or ドメイン名>` で起動するユーザー明示型スキル。
kotlin-implementer と code-reviewer を APPROVED が出るまで最大3回ループする**救済措置**。
通常は同一セッション内でメイン AI がこのフローを自動実行するが、
AI 側の理由でうまくループできない場合や改めてスキルとして実行したい場合に使う。

前提条件: db-designer / api-designer の設計書が作成済みで、人間が設計を承認済みであること。

→ **使い方**:
```
/implement-review-loop UC-R1
```

---

## 今後の流れ（Phase 2）

Step1実装サポート用マルチエージェント構成（db-designer / api-designer / kotlin-implementer / code-reviewer）は構築済み。
詳細・ワークフローは [exec-plans/active/0001-requirements-definition-multiagent.md](../exec-plans/active/0001-requirements-definition-multiagent.md) を参照。

次のステップ: Step1の実装フェーズ（Flywayマイグレーション → API設計 → Kotlin実装 → レビュー）を開始する。
