# ナビゲーション指標ログ

このファイルは、AIエージェント（主にClaudeCode）が `AGENTS.md` / `docs/README.md` を「目次」として
実際にどの程度効率よく使えているかを記録するログです。
[Harness Engineering（OpenAI記事）](../references/harness-engineering/openai-harness-engineering.md) で紹介されている
「AGENTS.mdを百科事典ではなく目次として扱う」という方針が、このリポジトリで機能しているかを
セッションを跨いで定量的に振り返るために使います。

## チェック・記録タイミング（実行バリエーション）

### 自動計測・チェック（SessionStart / PostToolUse / Stop hook）

セッション計測は以下の3つのhookが連携して動作する。

| hookイベント | スクリプト | 役割 |
|---|---|---|
| SessionStart | [`.claude/hooks/navigation-metrics-check.sh`](../../.claude/hooks/navigation-metrics-check.sh) | セッション開始時刻・ツールカウンタを初期化。前回セッションサマリーをコンテキストへ注入。閾値チェック・鮮度チェックを実行し、超過時に警告を注入 |
| PostToolUse | [`.claude/hooks/tool-counter.sh`](../../.claude/hooks/tool-counter.sh) | ツール呼び出し回数を `/tmp/claude_kakehashi_tool_count` にインクリメント |
| Stop | [`.claude/hooks/session-end.sh`](../../.claude/hooks/session-end.sh) | セッション終了時にツール数・所要時間を `/tmp/claude_kakehashi_last_session.json` に保存。次回SessionStart時に読み込まれ、navigation-metrics.md への記録を促す |

直近5件のうち3件以上で探索コストが3以上の場合、SessionStart時に警告メッセージがClaudeのコンテキストへ自動注入される。

### 詳細分析（手動: doc-maintainer）
`doc-maintainerサブエージェントでdocs/の整合性をチェックして` と依頼すると、
チェック項目8として閾値チェックと目次構成の具体的な見直し案が出る。

### 記録（ClaudeCode が追記）
- **誰が**: ClaudeCode（セッション内の作業を最もよく把握している主体）
- **推奨タイミング**:
  - PRを作成・マージした直後
  - ユーザーから明確な区切り（タスク完了・会話終了の意思表示）があったとき
- 毎ターン記録する必要はない。記録漏れは許容する（厳密な計測ツールではなく傾向を見るための簡易ログ）

## 指標の定義

### 難易度（1〜5）

そのセッションで扱ったタスクの複雑さ。

| 値 | 基準 |
|----|------|
| 1 | 単一ファイルの軽微な修正・確認のみ |
| 2 | 単一ドキュメント/モジュール内で完結する作業 |
| 3 | 複数ドキュメント・複数モジュールを横断する作業 |
| 4 | 複数の意思決定（ADR化が必要なレベル）を含む作業 |
| 5 | リポジトリ全体の構成・運用ルールに影響する設計判断 |

### 探索コスト（0〜5+）

目的の情報に到達するまでに、`AGENTS.md` / `docs/README.md` のリンクから**外れた**追加探索
（grepによる全文検索、リンクに載っていないファイルを推測で開く、など）が何回必要だったか。

| 値 | 基準 |
|----|------|
| 0 | `AGENTS.md` / `docs/README.md` / `memory` のリンクから一発で目的のドキュメントに到達 |
| 1〜2 | 軽微な追加探索（1〜2回のRead/Grepで解決） |
| 3+ | 目次に載っていない情報を探すために3回以上の追加探索が必要だった（= 目次が機能していない可能性） |

## 閾値ルール

直近5件のエントリのうち**3件以上**で探索コストが**3以上**の場合、
次回セッションの冒頭で `doc-maintainer` サブエージェントを呼び出し、
`AGENTS.md` / `docs/README.md` の目次構成の見直し案を提示することをユーザーに提案する。

## ログ

| 日付 | セッション概要 | 難易度 | 探索コスト | 備考 |
|------|------|------|------|------|
| 2026-06-15 | 要件定義の進行確認＋本ナビゲーション指標ログの設計・新設 | 3 | 0 | `memory`の要約から会話を継続でき、本タスクもAGENTS.md/docs/READMEのリンクから直接doc-maintainer定義等に到達できた |
| 2026-06-19 | AI-ADR-0006作成・CLAUDE.mdにdoc-maintainer必須ルール＋ADR自動提案ルール追加・docs/references/整理 | 4 | 1 | AGENTS.md/docs/READMEから必要なファイルにほぼ直接到達できた。doc-maintainerチェックをコミット後に実行するミスを2回。feedbackメモリに記録済み |
| 2026-06-23 | AssignRolesUseCase bugfix・エージェント/ルール構成リファクタリング（exec-plan 0002 Phase 1-7完了）・AI-ADR-0013・ハーネス/ガードレールドキュメント化・全体陳腐化チェック | 5 | 1 | AGENTS.md/.claude/agents/の索引から必要なファイルに直接到達できた。adr-governanceサブエージェントがセッション間でコンテキストを引き継げず3回再試行が必要だった（エージェント間の文脈共有の限界） |
