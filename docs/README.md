# Docs Index

このディレクトリのドキュメント導線です。

## まず見る

- [TODO](TODO.md)
- [ADR一覧（カテゴリ別索引）](adr/README.md)

## ADR

カテゴリ別索引・ステータス・次の番号は [adr/README.md](adr/README.md) を参照（正とする）。

## Conventions

- [Git Prefixes](conventions/git-prefixes.md)
- [Pre-push Test Check](conventions/pre-push-test-check.md)
- [Pre-commit Secret Check](conventions/pre-commit-secret-check.md)
- [PR Summary 自動生成](conventions/pr-summary-automation.md)
- [Markdown見出しアンカーへのリンク規約](conventions/markdown-anchor-links.md)
- [KDoc・テスト方針](conventions/kdoc-and-test-policy.md)

## ADR 作成方法

ADR の作成・更新・Supersede は **ADR Governance エージェント** が支援します。
ClaudeCodeでは `/adr-governance` スキル（または `adr-governance` サブエージェント）、
Copilotでは `@ADR Governance` を呼び出してください。

- テンプレートと命名規則・次の番号: [docs/adr/README.md](adr/README.md)

---

## Troubleshooting

- [Dev Container Compose Compatibility](troubleshooting/devcontainer-compose-compatibility.md)
- [Dev Container ClaudeCode Extension Missing](troubleshooting/devcontainer-claude-code-extension-missing.md)
- [Dev Container SSH Agent Forwarding](troubleshooting/devcontainer-ssh-agent-forwarding.md)
- [Gradle Java Home Invalid Folder](troubleshooting/gradle-javahome-invalid-folder.md)
- [Gradle Wrapper Lock Contention](troubleshooting/gradle-wrapper-lock-contention.md)
- [GitHub PR マージ状態 UNKNOWN](troubleshooting/github-merge-status-unknown.md)
- [Dev Container shellcheck nanolayer GLIBC エラー](troubleshooting/devcontainer-shellcheck-nanolayer-glibc.md)
- [Spring Boot 4.x + Testcontainers 統合テスト確定パターン](troubleshooting/testcontainers-jvmstatic-kotlin.md)
- [Spring Boot 4.x 移行に伴う CI 連鎖障害の調査記録](troubleshooting/spring-boot-4x-ci-failure-chain.md)

---

## Agents（マルチエージェント構成）

- [Agents概要・使い方（人間向け）](agents/README.md)
- [ナビゲーション指標ログ](agents/navigation-metrics.md) - 目次（AGENTS.md/docs/README.md）の機能度を振り返るログ
- [マルチエージェント構成 実行計画（exec-plan 0001）](exec-plans/completed/0001-requirements-definition-multiagent.md)


全体マップ（AIエージェント向け索引）は [AGENTS.md](../AGENTS.md) を参照。

## Exec Plans / Design Docs

- [exec-plans運用ルール](exec-plans/README.md) - 実行計画（進行中/完了/技術的負債）
- [design-docs/index.md](design-docs/index.md) - design-docs索引
- [design-docs/core-beliefs.md](design-docs/core-beliefs.md) - 運用原則・思想
- [design-docs/harness-and-guardrails.md](design-docs/harness-and-guardrails.md) - ハーネス（事前設計層）とガードレール（事後検証層）の構成

## Requirements（要件定義）

- [requirements/README.md](requirements/README.md) - プロジェクトコンテキスト・アーキテクチャ方針
- [requirements/ui-flows.md](requirements/ui-flows.md) - ユースケース・画面遷移（レビュー完了・要件定義確定）
- [requirements/data-models.md](requirements/data-models.md) - データモデル設計（レビュー完了・要件定義確定）
- [requirements/quality-standards.md](requirements/quality-standards.md) - 非機能要件・品質目標（レビュー完了・要件定義確定）

## Database（テーブルカタログ）

- [database/README.md](database/README.md) - 実装済みテーブル一覧・DDL/データモデル/ADRへのリンク集

## Design（API・詳細設計）

- [design/api/account-role.md](design/api/account-role.md) - REST API設計書: アカウント・ロールドメイン（UC-A1〜A7）

## Architecture（モジュール構成・パッケージ設計）

- [architecture/README.md](architecture/README.md) - モジュール構成・パッケージ設計・サブプロジェクト構成（Step1実装フェーズで随時更新）
- [architecture/package-structure.md](architecture/package-structure.md) - パッケージ構成規約（レイヤー・命名・UseCase DI・Enum 活用）

## References（外部参考資料）

- [references/README.md](references/README.md) - 外部参考資料フォルダの目次（収録ルール・一覧）
- [Harness Engineering（OpenAI記事転記）](references/harness-engineering/openai-harness-engineering.md) - エージェント設計の参考記事
