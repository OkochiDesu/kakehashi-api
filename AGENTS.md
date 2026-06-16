# AGENTS.md

このファイルはAIエージェント向けの「目次（マップ）」です。
詳細なルールや知識は各リンク先のドキュメントに記載されています。すべてをここに書き込まないでください。
目安として100行を超えそうになったら、内容を `docs/` 配下に移し、リンクに置き換えることを検討する。

## このリポジトリについて

kakehashi-api: Spring Boot (Kotlin/Gradle) で構築するAPIサーバー。現在は初期セットアップ・要件定義フェーズ。

## 最初に読むもの

| 目的 | リンク |
|------|--------|
| エージェントの行動ルール・権限設定 | [CLAUDE.md](CLAUDE.md) |
| ドキュメント全体の索引 | [docs/README.md](docs/README.md) |
| 未対応タスク一覧 | [docs/TODO.md](docs/TODO.md) |

## ドキュメント構成（docs/）

| ディレクトリ | 内容 |
|------|------|
| `docs/architecture/` | モジュール構成・パッケージ設計・サブプロジェクト構成（実装フェーズで具体化予定） |
| `docs/adr/` | アーキテクチャ決定record (ADR)。命名・運用ルールは [docs/adr/README.md](docs/adr/README.md) |
| `docs/conventions/` | コーディング規約・運用ルール |
| `docs/troubleshooting/` | 既知の問題と対処方法 |
| `docs/agents/` | マルチエージェント構成・エージェント関連の設計資料 |
| `docs/exec-plans/` | 実行計画（進行中/完了/技術的負債）。運用ルールは [docs/exec-plans/README.md](docs/exec-plans/README.md) |
| `docs/design-docs/` | 運用原則・思想（[core-beliefs.md](docs/design-docs/core-beliefs.md)） |
| `docs/requirements/` | 要件定義ドキュメント（UI・データモデル・品質目標）。索引は [docs/requirements/README.md](docs/requirements/README.md) |

新しいドキュメントを追加する場合は、適切なディレクトリに配置し、必ず [docs/README.md](docs/README.md) からリンクすること。

## 利用可能なサブエージェント（.claude/agents/）

| エージェント | 用途 |
|------|------|
| [doc-maintainer](.claude/agents/doc-maintainer.md) | `docs/` の整合性・索引・鮮度チェック |
| [adr-governance](.claude/agents/adr-governance.md) | ADRの作成・更新・Supersedeのオーケストレーター |
| [adr-search](.claude/agents/adr-search.md) | 変更に関連するADR候補の検索（adr-governanceから呼び出し） |
| [adr-validator](.claude/agents/adr-validator.md) | ADRドラフトのポリシー準拠検証（adr-governanceから呼び出し） |

## 利用可能なスキル（.claude/skills/）

| スキル | 用途 |
|------|------|
| [adr-governance](.claude/skills/adr-governance/SKILL.md) | `/adr-governance` で起動。ADRの作成・更新・Supersedeを行う |

## GitHub Copilot 用エージェント（.github/agents/, .github/skills/）

ADRの作成・更新・Supersedeは、Copilotでは `@ADR Governance` エージェントが支援する（互換のため維持、ClaudeCode版は上記参照）。詳細は [docs/adr/README.md](docs/adr/README.md) を参照。

## ワークフロー

### ドキュメント変更時
1. 変更対象のドキュメントを編集する。
2. `doc-maintainer` サブエージェントで索引・リンク整合をチェックする。
3. 必要であれば `docs/README.md` の索引を更新する。

### タスク開始時（exec-plan判定）
1. タスクが複数PR/複数セッションに渡るか判定する。
2. フルexec-plan（`docs/exec-plans/active/`）が必要か、軽量プラン（TodoWriteのみ）で十分かを人間に提案し、確認を取る。
3. 判定基準・更新タイミングは [docs/exec-plans/README.md](docs/exec-plans/README.md) を参照。

### 要件定義（準備中）
マルチエージェント構成の詳細は [docs/exec-plans/active/0001-requirements-definition-multiagent.md](docs/exec-plans/active/0001-requirements-definition-multiagent.md) を参照。

### セッション終了時（ナビゲーション指標記録）
ユーザーから明確な区切りがあったら、[docs/agents/navigation-metrics.md](docs/agents/navigation-metrics.md) に
そのセッションの難易度・探索コストを1行追記する。閾値を超えた場合は同ファイルのルールに従い対応を提案する。

## 禁止事項・安全設定

破壊的操作（`git push`、`rm`、`find -delete`、`rsync --delete`、外部ダウンロード等）の制限と、`git commit` の運用ルール（コミットメッセージと差分を提示し確認後にAIが実行可、`git push` は人間のみ）は [CLAUDE.md](CLAUDE.md) および [.claude/settings.json](.claude/settings.json) を参照。
