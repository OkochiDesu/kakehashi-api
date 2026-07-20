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
| 2026-06-24 | Spring Boot 4.x CI連鎖障害修正・Testcontainers 2.0.5移行・APP-ADR-0012作成・Copilot Rv対応8件（pre-commit awk改善・postgres版統一・横展開ルール追記）・PR #13マージ | 4 | 1 | AGENTS.md/docs/READMEから必要ファイルに直接到達できた。Copilot指摘への返信漏れが1回発生しCLAUDE.mdにルール追記。pre-commitのawk判定が3ラウンド（偽陰性→偽陽性→アノテーション順序）かかった点が探索コストを押し上げた |
| 2026-06-24 | KDocフォーマット統一・exec-plan PR単位分割（0004解体→0005〜0015新設）・TODO/exec-planすみ分け方針確立・doc-maintainerチェック7件修正・PR #14作成 | 5 | 1 | AGENTS.md/exec-plans/READMEから直接到達できた。active/→completed/リンク切れが3ラウンドのdoc-maintainerチェックで順次発覚（grep一発で全件検出できなかった点が探索コストを押し上げた） |
| 2026-06-25 | PR #14 Copilot Rv 24件対応（docs/ リンク修正・KDoc《テスト》メソッド名完全一致・★★正常/異常系★★廃止→《観　点》単位グループ構成へ統一）・返信投稿 | 3 | 0 | AGENTS.md/memory/から直接到達できた。コンテキスト圧縮による2回のセッション断裂があったが、作業状態をgrepで素早く再確認できた |
| 2026-06-28 | exec-plan 0017・0018 完了（account-domain-agent/feedback-harness-agent新設・5エージェント不明点確認プロセス追加・AI-ADR-0019/0020・devcontainer ポート整理・IDE警告修正・CLAUDE.md ハーネス化・整理） | 5 | 1 | AGENTS.md/exec-plansから直接到達できた。コンテキスト圧縮2回でセッション断裂したが、セッションサマリーにより再開コストは低かった。grill-me方針変更（スタンドアロン→エージェント埋め込み）など意思決定が多く難易度高 |
| 2026-07-19 | PR #21最終Copilot指摘4件対応（JwtAuthenticationFilter permitAllバイパスバグ修正・allowed-domainsコメント矛盾修正・KDoc規約にoverride例外追加）＋JWT/X-Account-Idなりすましリスクをexec-plan 0007へユーザー判断で引き継ぎ。PR #20指摘対応（APP-ADR-0016に中間DTO不変性を明記・ADR目次「日付」省略慣習をadr-rules.md/adr-validator.md/doc-maintainer-structure.mdに仕組み化）。exec-plan 0006完了→completed/移動、0020着手決定、0021（ハーネス・ガードレール見直し）起票、PR #22作成・マージ | 5 | 2 | AGENTS.md/exec-plansから直接到達できた。ADR目次の日付リンク省略が既存慣習か確認するため全45件のADRをgrepする追加探索が発生。なりすましリスク対応方針・ブランチ命名等、複数の判断をユーザーに確認しながら進めた |
| 2026-07-19 | exec-plan 0020完了（AccountエンティティのAPP-ADR-0015準拠class化・AccountRepositoryImplのAPP-ADR-0016準拠MyBatis統一、PR #23）。TDD red確認漏れをユーザー指摘→feedback-harness-agentでtest-rules.mdに仕組み化。PR #23レビュー7ラウンド対応（中間不整合の順序修正・accountId/operatorId整合性検証追加・KDocの`@property`/非自明override説明の規約化3ファイル反映・TDDルール4ステップ明確化・KDocパラメータ表記修正・テストメソッド名とKDoc不一致修正）。VS Codeポートパネル増殖の原因調査（実プロセスなしと特定）→devcontainer.jsonにremote.autoForwardPorts=false追加。exec-plan 0020→completed/、0021→active/へ移動 | 5 | 1 | AGENTS.md/exec-plansから直接到達できた。ポート増加調査はドキュメント目次外の領域（ss/ps/VS Codeポートパネル）だったが実プロセス不在の特定は数コマンドで完結。KDoc規約の3ファイル同時反映が2度目の発生となり、exec-plan 0021（ルール集約）の必要性を裏付ける追加事例となった |
