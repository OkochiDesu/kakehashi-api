# AGENTS.md

このファイルはAIエージェント向けの「目次（マップ）」です。
詳細なルールや知識は各リンク先のドキュメントに記載されています。すべてをここに書き込まないでください。
目安として100行を超えそうになったら、内容を `docs/` 配下に移し、リンクに置き換えることを検討する。

## このリポジトリについて

kakehashi-api: Spring Boot (Kotlin/Gradle) で構築するAPIサーバー。Step1（アカウント・ロールドメイン）実装完了済み。

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
| `docs/database/` | 実装済みテーブルカタログ。DDL・データモデル・ADRへのリンク集。索引は [docs/database/README.md](docs/database/README.md) |
| `docs/adr/` | アーキテクチャ決定record (ADR)。命名・運用ルールは [docs/adr/README.md](docs/adr/README.md) |
| `docs/conventions/` | コーディング規約・運用ルール |
| `docs/troubleshooting/` | 既知の問題と対処方法 |
| `docs/agents/` | マルチエージェント構成・エージェント関連の設計資料 |
| `docs/exec-plans/` | 実行計画（進行中/完了/技術的負債）。運用ルールは [docs/exec-plans/README.md](docs/exec-plans/README.md) |
| `docs/design-docs/` | 運用原則・思想（[core-beliefs.md](docs/design-docs/core-beliefs.md)） |
| `docs/requirements/` | 要件定義ドキュメント（UI・データモデル・品質目標）。索引は [docs/requirements/README.md](docs/requirements/README.md) |
| `docs/design/` | API・詳細設計書（REST APIエンドポイント設計等）。索引は [docs/README.md](docs/README.md) |
| `docs/references/` | 外部参考資料（記事転記・画像）。記事ごとにサブフォルダで管理 |

新しいドキュメントを追加する場合は、適切なディレクトリに配置し、必ず [docs/README.md](docs/README.md) からリンクすること。

## 利用可能なサブエージェント（.claude/agents/）

| エージェント | 用途 |
|------|------|
| [doc-maintainer-structure](.claude/agents/doc-maintainer-structure.md) | `docs/` の索引・リンク整合性・ToC チェック（コミット前軽量チェック用） |
| [doc-maintainer-content](.claude/agents/doc-maintainer-content.md) | `docs/` の ADR整合・exec-plans・design-docs・TODO実行可能性チェック（定期チェック用、structure と並列実行） |
| [doc-maintainer](.claude/agents/doc-maintainer.md) | `docs/` の全項目フルチェック（レガシー、上記2エージェントの並列実行を推奨） |
| [adr-governance](.claude/agents/adr-governance.md) | ADRの作成・更新・Supersedeのオーケストレーター |
| [adr-search](.claude/agents/adr-search.md) | 変更に関連するADR候補の検索（adr-governanceから呼び出し） |
| [adr-validator](.claude/agents/adr-validator.md) | ADR・AI-ADRドラフトのポリシー準拠検証（adr-governanceから呼び出し） |
| [db-designer](.claude/agents/db-designer.md) | Flywayマイグレーションスクリプトの設計・作成 |
| [api-designer](.claude/agents/api-designer.md) | REST APIエンドポイントの設計（kotlin-implementerへの入力） |
| [kotlin-implementer](.claude/agents/kotlin-implementer.md) | Spring Boot (Kotlin) 実装（Entity/Repository/Service/Controller） |
| [class-diagram-updater](.claude/agents/class-diagram-updater.md) | kotlin-implementer完了後に `src/` 配下のREADME.md（クラス図・関連図）を自動生成・更新 |
| [src-doc-maintainer](.claude/agents/src-doc-maintainer.md) | class-diagram-updater完了後に `src/` 内README.mdとコードの整合性をチェック |
| [design-impl-checker](.claude/agents/design-impl-checker.md) | API設計書（`docs/design/api/*.md`）とController実装のパス・リクエスト/レスポンス整合性をチェック |
| [code-reviewer](.claude/agents/code-reviewer.md) | 実装コードのレビュー。APPROVED/REQUIRES_CHANGESを明示し人間の最終確認を支援 |
| [test-reviewer](.claude/agents/test-reviewer.md) | テストコードのレビュー。code-reviewer APPROVED後に呼び出し、テスト品質・カバレッジ・監査カラム検証・楽観ロック競合テストを確認 |

## 利用可能な glob ルール（.claude/rules/）

ファイルパターンに一致するときのみ自動適用されるルール集。エージェント定義からも参照する。

| ルールファイル | 適用対象 | 内容 |
|------|------|------|
| [test-rules.md](.claude/rules/test-rules.md) | `**/*Test.kt` | TDD・アサーション種別・updatedAt検証・楽観ロック競合テスト・テスト命名 |
| [mybatis-rules.md](.claude/rules/mybatis-rules.md) | `**/*Mapper.xml`, `**/*Mapper.kt` | `<id>`タグ・`notNullColumn`・`#{}`使用・`@param`必須 |

## 利用可能なスキル（.claude/skills/）

| スキル | 用途 |
|------|------|
| [adr-governance](.claude/skills/adr-governance/SKILL.md) | `/adr-governance` で起動。ADR・AI-ADRの作成・更新・Supersedeを行う救済スキル（通常は同一セッション内でAIが自動的にadr-governanceサブエージェントを呼び出す） |
| [implement-review-loop](.claude/skills/implement-review-loop/SKILL.md) | `/implement-review-loop` で起動。kotlin-implementer→code-reviewer→test-reviewerをAPPROVEDまでループする救済スキル（通常は同一セッション内でAIが自動実行） |

## GitHub Copilot 用エージェント（.github/agents/, .github/skills/）

ADRの作成・更新・Supersedeは、Copilotでは `@ADR Governance` エージェントが支援する（互換のため維持、ClaudeCode版は上記参照）。詳細は [docs/adr/README.md](docs/adr/README.md) を参照。

## ワークフロー

### ドキュメント変更時
1. 変更対象のドキュメントを編集する。
2. `doc-maintainer-structure` サブエージェントで索引・リンク整合性・ToC をチェックする（コミット前軽量チェック）。新規ファイル追加を含む場合は `doc-maintainer-content` も並列で呼び出す。
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
