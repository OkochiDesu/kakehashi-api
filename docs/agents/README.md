# エージェント・ハーネス

ClaudeCode の動作を制御・拡張するハーネスの構成と、エージェント設計の原則をまとめる。
運用ナビゲーション指標は [navigation-metrics.md](navigation-metrics.md) を参照。

## 目次

- [エージェント定義の標準構成](#エージェント定義の標準構成)
- [ハーネス構成一覧](#ハーネス構成一覧)
- [動作保証サマリー](#動作保証サマリー)
- [エージェント設計ADR（AI-ADR）索引](#エージェント設計adraiadr索引)

## エージェント定義の標準構成

`.claude/agents/` 配下のエージェント定義は以下の構成を標準とする。

```
---
name: <エージェント名>
description: "<説明>"
tools: <ツール一覧>
model: <モデル名>
---

## 位置づけと呼び出しタイミング  ← 必須
...（役割固有のセクション・自由）...
## 出力フォーマット               ← 必須
```

中間セクション（`## 厳守ルール`・`## 作業手順`・`## チェック項目` 等）はエージェントの役割に応じて自由に構成してよい。

### チェックリスト出力を持つエージェント共通ルール

**`## 出力フォーマット` には全チェック項目を必ず列挙し、各項目にステータスを明記すること**（根拠: [AI-ADR-0018](../adr/AI-ADR-0018-レビュー系エージェントの全項目列挙出力パターン.md)）。

| 種別 | 対象エージェント例 | ステータス値 |
|---|---|---|
| レビュー系（APPROVED/REQUIRES_CHANGES を返す） | code-reviewer, test-reviewer | `PASS / FAIL / SKIP` |
| チェッカー系（OK/要対応/REQUIRES_FIX を返す） | design-impl-checker, doc-maintainer-*, src-doc-maintainer, adr-validator | `OK / 要対応 / SKIP` |

問題項目のみの列挙は禁止。チェックをスキップしても出力に現れないため信頼性が失われる。

## ハーネス構成一覧

### 設定・常時注入

| ファイル | 役割 |
|---|---|
| [`.claude/settings.json`](../../.claude/settings.json) | 権限設定（危険操作ブロック・`.env` 保護） |
| [`CLAUDE.md`](../../CLAUDE.md) | ClaudeCode の行動指針（禁止事項・推奨事項・commit 運用） |
| [`AGENTS.md`](../../AGENTS.md) | リポジトリ全体の目次（サブエージェント一覧・ドキュメント構成） |

### エージェント（`.claude/agents/`）

#### ドキュメント整備系

| エージェント | 役割 |
|---|---|
| [doc-maintainer-structure](../../.claude/agents/doc-maintainer-structure.md) | 索引・リンク整合性・`.claude/` 構成・ToC チェック（コミット前軽量チェック用） |
| [doc-maintainer-content](../../.claude/agents/doc-maintainer-content.md) | ADR整合・exec-plans・design-docs・TODO実行可能性チェック（structure と並列実行） |

#### ADR ガバナンス系

| エージェント | 役割 |
|---|---|
| [adr-governance](../../.claude/agents/adr-governance.md) | ADR 作成・更新・Supersede のオーケストレーター（model: opus） |
| [adr-search](../../.claude/agents/adr-search.md) | ADR 候補の検索（adr-governance から呼び出し、model: haiku） |
| [adr-validator](../../.claude/agents/adr-validator.md) | ADR ドラフトのポリシー準拠検証（adr-governance から呼び出し、model: haiku） |

#### 実装パイプライン系

| エージェント | 役割 |
|---|---|
| [db-designer](../../.claude/agents/db-designer.md) | Flyway マイグレーション設計・作成 |
| [api-designer](../../.claude/agents/api-designer.md) | REST API 設計書生成 |
| [test-scenario-planner](../../.claude/agents/test-scenario-planner.md) | API 設計承認後・実装前のテストシナリオ設計（人間承認ゲート） |
| [kotlin-implementer](../../.claude/agents/kotlin-implementer.md) | Spring Boot (Kotlin) 実装（Entity / Repository / Service / Controller） |
| [class-diagram-updater](../../.claude/agents/class-diagram-updater.md) | `src/` 配下 README.md のクラス図・関連図を自動更新（kotlin-implementer 後） |
| [src-doc-maintainer](../../.claude/agents/src-doc-maintainer.md) | `src/` 内 README.md とコードの整合性チェック（class-diagram-updater 後） |
| [design-impl-checker](../../.claude/agents/design-impl-checker.md) | API 設計書と Controller 実装のパス・リクエスト/レスポンス整合性チェック |

#### レビュー系（最終ゲート・model: opus）

| エージェント | 役割 |
|---|---|
| [code-reviewer](../../.claude/agents/code-reviewer.md) | 実装コードレビュー（ADR 準拠・セキュリティ・設計品質） |
| [test-reviewer](../../.claude/agents/test-reviewer.md) | テストコードレビュー（code-reviewer APPROVED 後） |

#### ドメイン番人系

| エージェント | 役割 |
|---|---|
| [account-domain-agent](../../.claude/agents/account-domain-agent.md) | アカウントドメインのビジネスルール番人。ステータス遷移・認可・楽観ロックの準拠を検証（api-designer/kotlin-implementer がアカウント触れる際に呼び出す） |

#### フィードバック対応系

| エージェント | 役割 |
|---|---|
| [feedback-harness-agent](../../.claude/agents/feedback-harness-agent.md) | ユーザー/GitHub Copilot の指摘を受けたとき、memory/CLAUDE.md/agents/.githooks への振り分けを分類・提案・実装する |

### グロブルール（`.claude/rules/`）

| ルール | 適用対象 | 内容 |
|---|---|---|
| [test-rules.md](../../.claude/rules/test-rules.md) | `**/*Test.kt` | テストコード規約（TDD・アサーション・KDoc） |
| [mybatis-rules.md](../../.claude/rules/mybatis-rules.md) | `**/*Mapper.xml`, `**/*Mapper.kt` | MyBatis 規約（`#{}`・`<id>`・`notNullColumn`） |
| [adr-rules.md](../../.claude/rules/adr-rules.md) | `**/*-ADR-*.md` | ADR 編集規約（命名・テンプレート・Supersede） |
| [exec-plan-rules.md](../../.claude/rules/exec-plan-rules.md) | `docs/exec-plans/**/*.md` | exec-plan 運用ルール（粒度・テンプレート・ワークフロー） |

### フック（`.claude/hooks/`）

| フック | タイミング | 役割 |
|---|---|---|
| [navigation-metrics-check.sh](../../.claude/hooks/navigation-metrics-check.sh) | SessionStart | 前回サマリー注入・閾値チェック |
| [tool-counter.sh](../../.claude/hooks/tool-counter.sh) | PostToolUse | ツール呼び出し回数カウント |
| [session-end.sh](../../.claude/hooks/session-end.sh) | Stop | ツール数・所要時間を集計・保存（次回 SessionStart へ引き継ぎ） |
| [docs-change-check.sh](../../.claude/hooks/docs-change-check.sh) | PostToolUse | `docs/` 変更後に doc-maintainer チェックリマインダーを表示 |

### スキル（`.claude/skills/`）

| スキル | 起動コマンド | 役割 |
|---|---|---|
| [adr-governance](../../.claude/skills/adr-governance/SKILL.md) | `/adr-governance` | ADR 作成・更新・Supersede の救済スキル |
| [implement-review-loop](../../.claude/skills/implement-review-loop/SKILL.md) | `/implement-review-loop` | test-scenario-planner → kotlin-implementer → code-reviewer → test-reviewer を APPROVED までループ |

## 動作保証サマリー

- **危険操作は自動ブロック**: `gh pr merge` / `git reset --hard` / `rm` / `curl` 等は `.claude/settings.json` で拒否済み
- **`.env` 保護**: `.env` 系ファイルの Read / Write は deny リスト登録済み
- **`git commit` / `git push` / `git merge`** は対象・範囲をユーザーに提示し、明示的な確認後に実行
- **`AGENTS.md` は常時注入**: `CLAUDE.md` の `@AGENTS.md` import により、セッション圧縮後も目次が保持される
- **ナビゲーション指標**: セッション開始・終了時にフックで自動計測。閾値超過時は警告注入（詳細: [navigation-metrics.md](navigation-metrics.md)）

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
| [AI-ADR-0014](../adr/AI-ADR-0014-AIのgit-gh操作権限を3層モデルで整理.md) | AIのgit/gh操作権限を3層モデル（自動・確認・ブロック）に整理 | Accepted |
| [AI-ADR-0015](../adr/AI-ADR-0015-タスク開始時にTodoWriteでプランを作成し見える範囲と見えない範囲を明示する.md) | タスク開始時にTodoWriteでプランを作成し見える範囲・見えない範囲を明示する | Accepted |
| [AI-ADR-0016](../adr/AI-ADR-0016-GitHub-CopilotとClaudeCodeの役割分担明確化とCopilotエージェントファイルの削除.md) | GitHub Copilot と ClaudeCode の役割分担明確化と Copilot エージェントファイルの削除 | Accepted |
| [AI-ADR-0017](../adr/AI-ADR-0017-レビュー系エージェントの最終ゲート原則によるモデル選定.md) | レビュー系エージェントの最終ゲート原則によるモデル選定（code-reviewer・test-reviewer を opus に） | Accepted |
| [AI-ADR-0018](../adr/AI-ADR-0018-レビュー系エージェントの全項目列挙出力パターン.md) | チェックリスト出力を持つエージェントの全項目列挙パターン（レビュー系: PASS/FAIL/SKIP・チェッカー系: OK/要対応/SKIP） | Accepted |
| [AI-ADR-0019](../adr/AI-ADR-0019-Qiitaセキュリティ設定棚卸し結果の記録.md) | Qiita セキュリティ設定棚卸し結果の記録（3項目の見送り判断） | Accepted |
| [AI-ADR-0020](../adr/AI-ADR-0020-feedback-harness-agentの導入.md) | feedback-harness-agent の導入（指摘受領時の5分類エージェント化） | Accepted |

> AI-ADR の追加・更新は `/adr-governance` スキルまたは `adr-governance` サブエージェントで行う。
