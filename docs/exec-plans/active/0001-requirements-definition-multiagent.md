# 0001: 要件定義用マルチエージェント構成の構築

> 旧 `docs/agents/multi-agent-setup-guide.md` を本exec-planに移行したもの。

## 目的・スコープ

要件定義フェーズに入る前に、ClaudeCodeの安全設定・ドキュメントマップ・ドキュメント整備エージェントを整備し、
その上で要件定義（コンテキスト収集・ドメイン分析・要件ドラフト・レビュー）を行うマルチエージェント構成を構築する。

参考: [Harness Engineering（OpenAI記事）](../../agents/openAI_harness_enjineerring.md)

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

### Phase 2: 要件定義用マルチエージェント構成（未着手）
- [ ] システムコンテキストの共有（目的・ドメイン・制約をユーザーから受領）
- [ ] サブエージェントA〜Dの定義・作成
- [ ] オーケストレーター用ワークフロー定義
- [ ] 動作確認（小さなタスクで試運転）

## 構成概要（Phase 2）

```
ユーザー
  │
  ▼
[オーケストレーターエージェント]  ← メインエージェント
  │
  ├──▶ [コンテキスト収集エージェント]   サブエージェント A
  ├──▶ [ドメイン分析エージェント]       サブエージェント B
  ├──▶ [要件ドラフトエージェント]       サブエージェント C
  ├──▶ [レビュー・検証エージェント]     サブエージェント D
  └──▶ [doc-maintainer]                サブエージェント E（実装済み）
```

| サブエージェント | 役割 | 入力 | 出力 | ツール |
|---|---|---|---|---|
| A: コンテキスト収集 | 要件定義に必要な情報の特定・不足情報の質問リスト生成 | ユーザーの初期説明 | 確認事項リスト・収集済み情報のサマリ | Read |
| B: ドメイン分析 | 業務ドメインの分析・用語定義・ビジネスルールの抽出 | 収集済みコンテキスト | ドメインモデル草案・用語集・ビジネスルール一覧 | Read, WebSearch |
| C: 要件ドラフト | 機能要件・非機能要件・制約条件の文書化 | ドメイン分析結果 | 要件定義書ドラフト | Read, Write |
| D: レビュー・検証 | 要件の矛盾検出・抜け漏れ確認・品質チェック | 要件定義書ドラフト | レビューコメント・修正提案 | Read |
| E: doc-maintainer | `docs/` の索引・整合性・鮮度チェック | 生成・更新されたドキュメント | OK / 要対応リスト | Read, Grep, Glob |

オーケストレーターのワークフロー:
1. ユーザーからコンテキストを受け取る
2. コンテキスト収集エージェント(A)を起動し、不足情報を洗い出す
3. ドメイン分析エージェント(B)を起動し、業務ルール・制約を整理する
4. 要件ドラフトエージェント(C)を起動し、機能要件・非機能要件を草案する
5. レビューエージェント(D)を起動し、矛盾・抜け漏れを検証する
6. doc-maintainer(E)を起動し、生成したドキュメントの索引・リンク整合をチェックする
7. 統合して最終成果物を出力する

## 意思決定ログ

- 2026-06-09: ClaudeCodeの安全設定として `.claude/settings.json` の deny リストに `git commit`/`push`、`rm`系、`find -delete`/`rsync --delete`、`curl`/`wget`を追加。理由: 初期セットアップ段階での不要な破壊的操作・外部ダウンロードを防止するため。
- 2026-06-13: AGENTS.mdを「百科事典ではなく目次（マップ）」として採用。理由: Harness Engineering記事の「コンテキストは希少資源」「マニュアルは即座に腐る」という教訓に基づく。本文は `docs/` 配下に記載し、AGENTS.mdからリンクする方針。
- 2026-06-13: `doc-maintainer` を読み取り専用（Read, Grep, Glob）のチェッカーとして最小実装。理由: 記事の「ドキュメント整備エージェント」を、まずは安全な読み取り専用から導入するため。
- 2026-06-13: ADR Governanceエージェント（Copilot版）をClaudeCode用に変換しつつ、Copilot版（`.github/agents/`, `.github/skills/`）は削除せず互換維持。理由: 既存のCopilotユーザーへの影響を避けるため。
- 2026-06-13: exec-plansのファイル命名は4桁連番（ADRと同方式）。design-docs/exec-plansの整合性チェックは新規エージェントを作らず `doc-maintainer` を拡張する。理由: エージェント数を増やさず「docs/全体の整合性チェック」という役割に一貫させるため。
- 2026-06-13: タスク規模（フルexec-plan / 軽量プラン）の判定は、ClaudeCodeが提案し人間が確認する形にする。理由: 1機能・1PR単位でのヒューマンインザループを維持するため。
- 2026-06-13: Phase 1.7（exec-plans/design-docs整備）完了。doc-maintainerのチェック項目拡張、AGENTS.md/CLAUDE.md/docs/README.md/docs/agents/README.mdの参照更新まで実施済み。
- 2026-06-13: `.claude/settings.json` の deny リストから `Bash(git commit*)` を削除。CLAUDE.mdに「commit運用」セクションを新設し、コミットメッセージと `git diff --cached` をユーザーに提示して確認を得た場合のみAIが `git commit` を実行できる運用に変更（`git push` は引き続きdeny・人間のみ）。追加防御として `.githooks/pre-commit` によるシークレット簡易チェックを導入（[docs/conventions/pre-commit-secret-check.md](../../conventions/pre-commit-secret-check.md)）。ADR governanceエージェント/スキルは従来通りステージングまでとし、commitの判断は呼び出し元に委ねる方針に統一。

## 残課題・引き継ぎ事項

- システムコンテキスト（目的・ドメイン・制約）の共有は、本exec-planの整備が完了してから行う。
- Phase 2のサブエージェントA〜Dは、システムコンテキスト受領後に定義する。
