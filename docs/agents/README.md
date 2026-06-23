# Agents ディレクトリ概要（人間向け）

このディレクトリ配下と、リポジトリルートに追加した設定によって「今できるようになったこと」と「使い方」をまとめる。

## 目次

- [追加したファイルと役割](#追加したファイルと役割)
- [今できるようになったこと](#今できるようになったこと)
  - [1. 危険な操作の自動ブロック](#1-危険な操作の自動ブロック)
  - [2. リポジトリの「地図」が常に読み込まれる](#2-リポジトリの地図が常に読み込まれる)
  - [3. ドキュメント整備サブエージェント（doc-maintainer）](#3-ドキュメント整備サブエージェントdoc-maintainer)
  - [4. 実行計画（exec-plans）と運用原則（design-docs）](#4-実行計画exec-plansと運用原則design-docs)
  - [5. ナビゲーション指標ログ（navigation-metrics.md）](#5-ナビゲーション指標ログnavigation-metricsmd)
  - [6. 会話圧縮後もAGENTS.mdの内容を保持](#6-会話圧縮後もagentsmの内容を保持)
  - [7. 実装・レビューループスキル（implement-review-loop）](#7-実装レビューループスキルimplement-review-loop)
- [今後の流れ（Phase 2）](#今後の流れphase-2)

## 追加したファイルと役割

| ファイル | 役割 | 誰が読むか |
|------|------|------|
| [`.claude/settings.json`](../../.claude/settings.json) | ClaudeCodeの権限設定。`git push`、`rm`、`find -delete`、`rsync --delete`、`curl`/`wget` 等を禁止。`git commit` はdenyリストに含めず、CLAUDE.mdの「commit運用」に従い都度確認のうえ実行可 | ClaudeCode（自動適用） |
| [`CLAUDE.md`](../../CLAUDE.md) | ClaudeCodeの行動指針（禁止事項・推奨事項） | ClaudeCode（自動で読み込まれる） |
| [`AGENTS.md`](../../AGENTS.md) | リポジトリ全体の「目次」。docsの構成・サブエージェント一覧へのリンク集 | AIエージェント全般（自動で読み込まれる） |
| [`.claude/agents/doc-maintainer-structure.md`](../../.claude/agents/doc-maintainer-structure.md) | 索引・リンク整合性・`.claude/`構成・ToCチェック（コミット前軽量チェック用） | ClaudeCode（呼び出すと動く） |
| [`.claude/agents/doc-maintainer-content.md`](../../.claude/agents/doc-maintainer-content.md) | ADR整合・exec-plans・design-docs・TODO実行可能性チェック（定期チェック用、structureと並列実行） | ClaudeCode（呼び出すと動く） |
| [`.claude/agents/doc-maintainer.md`](../../.claude/agents/doc-maintainer.md) | 全項目フルチェック（レガシー、上記2エージェントの並列実行を推奨） | ClaudeCode（呼び出すと動く） |
| [`.claude/agents/adr-governance.md`](../../.claude/agents/adr-governance.md) | ADR作成・更新・Supersedeのオーケストレーター（model: opus） | ClaudeCode（呼び出すと動く） |
| [`.claude/agents/adr-search.md`](../../.claude/agents/adr-search.md) | ADR候補の検索（adr-governanceから呼び出し、model: haiku） | ClaudeCode（呼び出すと動く） |
| [`.claude/agents/adr-validator.md`](../../.claude/agents/adr-validator.md) | ADRドラフトのポリシー準拠検証（adr-governanceから呼び出し、model: haiku） | ClaudeCode（呼び出すと動く） |
| [`.claude/agents/db-designer.md`](../../.claude/agents/db-designer.md) | Flywayマイグレーション設計・作成エージェント | ClaudeCode（呼び出すと動く） |
| [`.claude/agents/api-designer.md`](../../.claude/agents/api-designer.md) | REST API設計書生成エージェント | ClaudeCode（呼び出すと動く） |
| [`.claude/agents/kotlin-implementer.md`](../../.claude/agents/kotlin-implementer.md) | Spring Boot (Kotlin) 実装エージェント | ClaudeCode（呼び出すと動く） |
| [`.claude/agents/class-diagram-updater.md`](../../.claude/agents/class-diagram-updater.md) | `src/` 配下の Kotlin コードからクラス図・関連図を生成し、各パッケージの README.md を更新する（kotlin-implementer完了後に自動呼び出し） | ClaudeCode（呼び出すと動く） |
| [`.claude/agents/src-doc-maintainer.md`](../../.claude/agents/src-doc-maintainer.md) | `src/` 内 README.md とソースコードの整合性チェック（読み取り専用、class-diagram-updater完了後に呼び出し） | ClaudeCode（呼び出すと動く） |
| [`.claude/agents/design-impl-checker.md`](../../.claude/agents/design-impl-checker.md) | API設計書とController実装の整合性チェック（読み取り専用）。パス・HTTPメソッド・リクエスト/レスポンス一致を検証し、不整合があれば対応方針をユーザーに確認する | ClaudeCode（呼び出すと動く） |
| [`.claude/agents/test-scenario-planner.md`](../../.claude/agents/test-scenario-planner.md) | テストシナリオ設計エージェント（API設計書承認後・kotlin-implementer呼び出し前に実行し、人間が承認するゲートを設ける） | ClaudeCode（呼び出すと動く） |
| [`.claude/agents/code-reviewer.md`](../../.claude/agents/code-reviewer.md) | 実装コードレビューエージェント | ClaudeCode（呼び出すと動く） |
| [`.claude/agents/test-reviewer.md`](../../.claude/agents/test-reviewer.md) | テストコードレビューエージェント（code-reviewer APPROVED後に呼び出し） | ClaudeCode（呼び出すと動く） |
| [`.claude/rules/test-rules.md`](../../.claude/rules/test-rules.md) | `*Test.kt` 編集時に自動適用されるテストコード規約（globs: `**/*Test.kt`） | ClaudeCode（glob一致時に自動適用） |
| [`.claude/rules/mybatis-rules.md`](../../.claude/rules/mybatis-rules.md) | `*Mapper.xml`/`*Mapper.kt` 編集時に自動適用されるMyBatis規約 | ClaudeCode（glob一致時に自動適用） |
| [`.claude/skills/adr-governance/SKILL.md`](../../.claude/skills/adr-governance/SKILL.md) | `/adr-governance` スキル。ADR・AI-ADRの作成・更新・Supersedeを行う救済スキル（通常は同一セッション内でAIが自動的にadr-governanceサブエージェントを呼び出す） | ClaudeCode（`/adr-governance` で起動） |
| [`.claude/skills/implement-review-loop/SKILL.md`](../../.claude/skills/implement-review-loop/SKILL.md) | `/implement-review-loop` スキル。test-scenario-planner→kotlin-implementer→code-reviewer→test-reviewerをAPPROVEDまでループ | ClaudeCode（`/implement-review-loop` で起動） |
| [`.claude/hooks/navigation-metrics-check.sh`](../../.claude/hooks/navigation-metrics-check.sh) | SessionStart時にセッション計測を開始し、前回サマリー注入・閾値チェック・鮮度チェックを実行 | ClaudeCode（自動実行） |
| [`.claude/hooks/tool-counter.sh`](../../.claude/hooks/tool-counter.sh) | PostToolUse時にツール呼び出し回数をカウント（`/tmp/claude_kakehashi_tool_count` に記録） | ClaudeCode（自動実行） |
| [`.claude/hooks/session-end.sh`](../../.claude/hooks/session-end.sh) | Stop時にツール数・所要時間を集計し次回SessionStartへ引き継ぐ（`/tmp/claude_kakehashi_last_session.json` に保存） | ClaudeCode（自動実行） |
| [`.claude/hooks/docs-change-check.sh`](../../.claude/hooks/docs-change-check.sh) | PostToolUse時に `docs/` 配下ファイルを変更したセッションで doc-maintainer チェックリマインダーを一度だけ表示 | ClaudeCode（自動実行） |
| [`docs/exec-plans/`](../exec-plans/README.md) | 実行計画（進行中/完了/技術的負債）の運用ルールと実体 | 人間（あなた）・Claude |
| [`docs/design-docs/core-beliefs.md`](../design-docs/core-beliefs.md) | このリポジトリの運用原則・思想 | 人間（あなた）・Claude |
| [`references/harness-engineering/`](../references/harness-engineering/openai-harness-engineering.md) | 参考にした元記事の転記（Harness Engineering） | 人間（あなた）・Claude |

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
| **セッション開始時（自動）** | SessionStart hook (`navigation-metrics-check.sh`) | 前回セッションサマリーをコンテキスト注入 + 閾値超過時に警告注入 |
| **ツール呼び出し毎（自動）** | PostToolUse hook (`tool-counter.sh`) | ツール呼び出し回数をカウント |
| **セッション終了時（自動）** | Stop hook (`session-end.sh`) | ツール数・所要時間を集計・保存し次回SessionStartへ引き継ぐ |
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
test-scenario-planner → kotlin-implementer → code-reviewer → test-reviewer を APPROVED が出るまで最大3回ループする**救済措置**。
通常は同一セッション内でメイン AI がこのフローを自動実行するが、
AI 側の理由でうまくループできない場合や改めてスキルとして実行したい場合に使う。

前提条件: db-designer / api-designer の設計書が作成済みで、人間が設計を承認済みであること。test-scenario-planner によるシナリオ承認ゲートもループ内に含まれる。

→ **使い方**:
```
/implement-review-loop UC-R1
```

---

---

## エージェント設計ADR（AI-ADR）索引

エージェント構成のアーキテクチャ決定は `docs/adr/` に `AI-ADR-XXXX-` プレフィックスで記録する。
「なぜこの構成を選んだか」を後から辿れるよう、ここに索引を置く。

| AI-ADR | タイトル | ステータス |
|---|---|---|
| [AI-ADR-0001](../adr/AI-ADR-0001-Step1実装サポート用マルチエージェントパイプライン構成の採用.md) | Step1実装サポート用マルチエージェントパイプライン構成の採用 | Accepted |
| [AI-ADR-0002](../adr/AI-ADR-0002-ADR-Governanceエージェントの3層構造採用.md) | ADR Governanceエージェントの3層構造採用（オーケストレーター＋検索＋検証） | Accepted |
| [AI-ADR-0003](../adr/AI-ADR-0003-doc-maintainerの読み取り専用チェッカー設計.md) | doc-maintainerの読み取り専用チェッカー設計 | Superseded → AI-ADR-0011 |
| [AI-ADR-0004](../adr/AI-ADR-0004-implement-review-loopスキルの救済措置としての位置づけ.md) | implement-review-loopスキルの救済措置としての位置づけ | Accepted |
| [AI-ADR-0005](../adr/AI-ADR-0005-スキルの救済措置パターンと設計原則.md) | `.claude/skills/` の救済措置パターン（全スキル共通設計原則） | Accepted |
| [AI-ADR-0006](../adr/AI-ADR-0006-doc-maintainerチェックのCLAUDE.md明示方式採用.md) | doc-maintainer陳腐化チェックのCLAUDE.md明示方式採用 | Accepted |
| [AI-ADR-0007](../adr/AI-ADR-0007-AGENTS.mdを目次型マップとして採用.md) | AGENTS.mdを目次型（マップ）として採用 | Accepted |
| [AI-ADR-0008](../adr/AI-ADR-0008-AIのcommit権限をCLAUDE.md確認制御とpre-commit-fail-closedで管理.md) | AIのcommit権限をCLAUDE.md確認制御+pre-commit fail-closedで管理 | Accepted |
| [AI-ADR-0009](../adr/AI-ADR-0009-CLAUDE.md-importによるコンテキスト常時注入.md) | CLAUDE.md @importによるコンテキスト常時注入（hook廃止） | Accepted |
| [AI-ADR-0010](../adr/AI-ADR-0010-src配下README自動生成によるHITL可視性確保.md) | src配下README自動生成によるHITL可視性確保 | Accepted |
| [AI-ADR-0011](../adr/AI-ADR-0011-doc-maintainerの構造チェックと内容チェックへの分割.md) | doc-maintainerの構造チェックと内容チェックへの分割 | Accepted |
| [AI-ADR-0012](../adr/AI-ADR-0012-エラーメッセージ日本語化の横展開チェックをcode-reviewerエージェント内grepで行う.md) | エラーメッセージ日本語化の横展開チェックをcode-reviewerエージェント内grepで行う | Accepted |
| [AI-ADR-0013](../adr/AI-ADR-0013-LLMとスクリプトの役割分離とglobルール採用とtest-reviewer順次分離.md) | LLMとスクリプトの役割分離・globルール採用・test-reviewer順次分離 | Accepted |

> AI-ADR の追加・更新は `/adr-governance` スキルまたは `adr-governance` サブエージェントで行う。

---

## 今後の流れ（Phase 2）

Step1実装サポート用マルチエージェント構成（db-designer / api-designer / kotlin-implementer / code-reviewer）は構築済み。
詳細・ワークフローは [exec-plans/active/0001-requirements-definition-multiagent.md](../exec-plans/active/0001-requirements-definition-multiagent.md) を参照。

Step1実装フェーズ（Flywayマイグレーション → API設計 → Kotlin実装 → レビュー）は完了済み（2026-06-22）。次のステップは deactivated 自動遷移バッチ・カバレッジ設定・Controller 統合テスト戦略（詳細は [docs/TODO.md](../../docs/TODO.md) 参照）。
