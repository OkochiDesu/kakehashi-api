# 0001: 要件定義用マルチエージェント構成の構築

> 旧 `docs/agents/multi-agent-setup-guide.md` を本exec-planに移行したもの。

## 目次

- [目的・スコープ](#目的スコープ)
- [進捗状況](#進捗状況)
  - [Phase 1: ClaudeCode 安全設定](#phase-1-claudecode-安全設定破壊的操作の制限)
  - [Phase 1.5: ドキュメントマップ & ドキュメント整備エージェント](#phase-15-ドキュメントマップ--ドキュメント整備エージェント)
  - [Phase 1.6: ADR Governanceエージェントの ClaudeCode 化](#phase-16-adr-governanceエージェントの-claudecode-化)
  - [Phase 1.7: exec-plans / design-docs の整備](#phase-17-exec-plans--design-docs-の整備本exec-plan自身を含む)
  - [Phase 2: Step1実装サポート用マルチエージェント構成](#phase-2-step1実装サポート用マルチエージェント構成)
- [構成概要（Phase 2: Step1実装サポート）](#構成概要phase-2-step1実装サポート)
- [意思決定ログ](#意思決定ログ)
- [残課題・引き継ぎ事項](#残課題引き継ぎ事項)

## 目的・スコープ

要件定義フェーズに入る前に、ClaudeCodeの安全設定・ドキュメントマップ・ドキュメント整備エージェントを整備し、
その上で要件定義（コンテキスト収集・ドメイン分析・要件ドラフト・レビュー）を行うマルチエージェント構成を構築する。

参考: [Harness Engineering（OpenAI記事）](../../references/harness-engineering/openai-harness-engineering.md)

## 進捗状況

### Phase 1: ClaudeCode 安全設定（破壊的操作の制限）
- [x] `.claude/settings.json` の作成（permissions.allow / deny）
- [x] `CLAUDE.md` の作成（行動指針）

### Phase 1.5: ドキュメントマップ & ドキュメント整備エージェント
- [x] `AGENTS.md`（マップ方式の目次）の作成
- [x] `.claude/agents/doc-maintainer.md`（読み取り専用ドキュメント整備エージェント）の作成

### Phase 1.6: ADR Governanceエージェントの ClaudeCode 化
- [x] `.github/agents/adr/*.agent.md`（Copilot用）を参照し、ClaudeCode用に変換
  - [.claude/agents/adr-governance.md](../../../.claude/agents/adr-governance.md)
  - [.claude/agents/adr-search.md](../../../.claude/agents/adr-search.md)
  - [.claude/agents/adr-validator.md](../../../.claude/agents/adr-validator.md)
  - [.claude/skills/adr-governance/SKILL.md](../../../.claude/skills/adr-governance/SKILL.md)
- [x] `docs/adr/README.md` / `docs/README.md` / `AGENTS.md` の参照を更新（Copilot版は互換のため維持）

### Phase 1.7: exec-plans / design-docs の整備（本exec-plan自身を含む）
- [x] `docs/exec-plans/`（`README.md` / `active/` / `completed/` / `tech-debt-tracker.md`）の作成
- [x] 本ファイル（旧 multi-agent-setup-guide.md）を exec-plan として移行
- [x] `docs/design-docs/core-beliefs.md` の作成
- [x] `doc-maintainer` のチェック項目に exec-plans / design-docs を追加
- [x] `AGENTS.md` / `CLAUDE.md` / `docs/README.md` 等の参照更新

### Phase 2: Step1実装サポート用マルチエージェント構成
- [x] システムコンテキストの共有（目的・ドメイン・制約をユーザーから受領）
- [x] 実装サポート用サブエージェントの定義・作成（db-designer / api-designer / kotlin-implementer / code-reviewer）
- [x] 動作確認（小さなタスクで試運転）

> **方針変更（2026-06-16）**: Step1要件定義が概ね完了しているため、Phase 2のサブエージェントを「要件定義用」から「Step1実装サポート用」に刷新した。
> 要件定義用エージェント（コンテキスト収集・ドメイン分析・要件ドラフト・レビュー）はStep2開始時または手戻り発生時に別途作成する。
> ワークフロー: `db-designer` → `api-designer` → `kotlin-implementer` → `class-diagram-updater` → `src-doc-maintainer` → `code-reviewer` → 人間確認 → commit

> **動作確認完了（2026-06-22）**: Step1アカウント・ロールドメイン実装（Kotlin 20ファイル超・テスト16ファイル・Flyway SQL 2ファイル）でパイプラインの試運転が完了した。

## 構成概要（Phase 2: Step1実装サポート）

```
ユーザー
  │（機能単位で指示）
  ▼
[db-designer]           DB スキーマ設計・Flyway SQL 生成
  │
  ▼
[api-designer]          REST API エンドポイント設計書生成
  │
  ▼
[kotlin-implementer]    Spring Boot 実装（Entity/Repository/Service/Controller + テスト）
  │
  ▼
[class-diagram-updater] src/ 配下 README.md のクラス図自動生成・更新
  │
  ▼
[src-doc-maintainer]    src/ 内 README.md とコードの整合性チェック（読み取り専用）
  │
  ▼
[code-reviewer]         ADR・セキュリティ・仕様適合レビュー → APPROVED/REQUIRES_CHANGES
  │
  ▼
人間確認 → commit
```

| サブエージェント | 役割 | 入力 | 出力 | ツール |
|---|---|---|---|---|
| db-designer | Flyway マイグレーション SQL 設計・作成 | data-models.md / ADR | `V*.sql` | Read, Grep, Glob, Write |
| api-designer | REST API エンドポイント設計書生成 | ui-flows.md / data-models.md | `docs/design/api/*.md` | Read, Grep, Glob, Write |
| kotlin-implementer | Spring Boot (Kotlin) 実装 | API 設計書 / data-models.md | Kotlin コード + テスト | Read, Write, Edit, Bash |
| class-diagram-updater | src/ 配下 README.md のクラス図自動生成・更新 | 実装コード | `src/***/README.md` | Read, Write, Edit |
| src-doc-maintainer | src/ 内 README.md とコードの整合性チェック（読み取り専用） | 実装コード + README.md | OK / REQUIRES_FIX レポート | Read, Grep, Glob |
| code-reviewer | ADR・セキュリティ・仕様適合レビュー | 実装コード | APPROVED/REQUIRES_CHANGES レポート | Read, Grep, Glob, Bash |
| doc-maintainer-structure | `docs/` の索引・リンク整合性・ToC チェック（コミット前） | 変更ドキュメント | OK / 要対応リスト | Read, Grep, Glob |
| doc-maintainer-content | `docs/` の ADR整合・exec-plans・TODO実行可能性チェック（定期） | docs/ 全体 | OK / 要対応リスト | Read, Grep, Glob |

実装サポートワークフロー:
1. db-designer を起動し、Flyway マイグレーション SQL を設計・生成する
2. api-designer を起動し、対象 UC の REST API 設計書（`docs/design/api/*.md`）を生成する
3. 人間が設計書を確認し、承認する
4. kotlin-implementer を起動し、Entity/Repository/Service/Controller + テストを実装する
4.5. class-diagram-updater を起動し、src/ 配下の README.md（クラス図・関連図）を自動生成・更新する
4.6. src-doc-maintainer を起動し、README.md とコードの整合性を確認する（読み取り専用）
5. code-reviewer を起動し、実装コードをレビューする（ADR・OWASP Top 10・仕様適合）
6. REQUIRES_CHANGES の場合は手順4に戻り、最大3回ループする（救済措置: `/implement-review-loop` スキル）
7. code-reviewer が APPROVED を出したら、人間に最終確認・commit を求める

## 意思決定ログ

- 2026-06-09: ClaudeCodeの安全設定として `.claude/settings.json` の deny リストに `git commit`/`push`、`rm`系、`find -delete`/`rsync --delete`、`curl`/`wget`を追加。理由: 初期セットアップ段階での不要な破壊的操作・外部ダウンロードを防止するため。
- 2026-06-13: AGENTS.mdを「百科事典ではなく目次（マップ）」として採用 → [AI-ADR-0007](../../adr/AI-ADR-0007-AGENTS.mdを目次型マップとして採用.md)
- 2026-06-13: `doc-maintainer` を読み取り専用チェッカーとして最小実装 → [AI-ADR-0003](../../adr/AI-ADR-0003-doc-maintainerの読み取り専用チェッカー設計.md)
- 2026-06-13: ADR Governanceエージェント（Copilot版）をClaudeCode用に変換、Copilot版は互換維持 → [AI-ADR-0002](../../adr/AI-ADR-0002-ADR-Governanceエージェントの3層構造採用.md)
- 2026-06-13: exec-plansのファイル命名は4桁連番（ADRと同方式）。design-docs/exec-plansの整合性チェックは新規エージェントを作らず `doc-maintainer` を拡張する。理由: エージェント数を増やさず「docs/全体の整合性チェック」という役割に一貫させるため。
- 2026-06-13: タスク規模（フルexec-plan / 軽量プラン）の判定は、ClaudeCodeが提案し人間が確認する形にする。理由: 1機能・1PR単位でのヒューマンインザループを維持するため。
- 2026-06-13: Phase 1.7（exec-plans/design-docs整備）完了。doc-maintainerのチェック項目拡張、AGENTS.md/CLAUDE.md/docs/README.md/docs/agents/README.mdの参照更新まで実施済み。
- 2026-06-13: AIのcommit権限をCLAUDE.md確認制御に移行し、pre-commit fail-closedを導入 → [AI-ADR-0008](../../adr/AI-ADR-0008-AIのcommit権限をCLAUDE.md確認制御とpre-commit-fail-closedで管理.md)
- 2026-06-13: commit運用に「ブランチ自動切り替え」を追加。commit前に現在ブランチのPR状態を `gh pr view --json state,number` で確認し、マージ済みの場合はユーザーに新ブランチの意図を確認した上で `feature/<内容>` ブランチを作成・切り替えてからcommitする。理由: 本リポジトリはPRをsquash-mergeしているため、git上の祖先関係チェックでは「mainにマージ済みか」を正しく判定できず、マージ済みブランチでの作業継続を防ぐ必要がある。`feature/` をブランチ名プレフィックスの正式ルールとして [Git Prefixes](../../conventions/git-prefixes.md) に追記。`gh pr view` を使うため、devcontainerに `ghcr.io/devcontainers/features/github-cli:1` を追加（コンテナのリビルドが必要）。
- 2026-06-15: CLAUDE.md先頭への `@AGENTS.md` import追加、sessionフック廃止 → [AI-ADR-0009](../../adr/AI-ADR-0009-CLAUDE.md-importによるコンテキスト常時注入.md)
- 2026-06-15: hookによる自動化見送り、doc-maintainerのチェック項目を拡張（チェック項目9: `.claude/`構成との整合性確認）。理由: エージェント数を増やさず「docs/全体の整合性チェック」という役割に一貫させるため。
- 2026-06-16: Phase 2サブエージェントをStep1実装サポート用に刷新 → [AI-ADR-0001](../../adr/AI-ADR-0001-Step1実装サポート用マルチエージェントパイプライン構成の採用.md)
- 2026-06-22: class-diagram-updater / src-doc-maintainer / design-impl-checker を追加しパイプラインを拡張 → [AI-ADR-0010](../../adr/AI-ADR-0010-src配下README自動生成によるHITL可視性確保.md)
- 2026-06-22: doc-maintainer を索引・リンク系（structure）と ADR・exec-plans系（content）の2エージェントに分割し、コミット前は軽量チェック・定期チェックは並列実行する方式を採用

## 残課題・引き継ぎ事項

- Step1実装フェーズ（Flywayマイグレーション → API設計 → Kotlin実装 → レビュー）を開始する。
- 要件定義用エージェント（コンテキスト収集・ドメイン分析・要件ドラフト・レビュー）は、Step2開始時または手戻り発生時に定義する。
