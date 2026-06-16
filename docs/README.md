# Docs Index

このディレクトリのドキュメント導線です。

## まず見る

- [TODO](TODO.md)
- [ADR一覧（カテゴリ別索引）](adr/README.md)

## ADR

- [ADR-0001: CI品質ゲートとDependabot運用方針](adr/ADR-0001-CI品質ゲートとDependabot運用方針.md)
- [ADR-0002: CIトリガー分離とWorkflow検証運用方針](adr/ADR-0002-CIトリガー分離とWorkflow検証運用方針.md)
- [ADR-0003: 複雑度しきい値によるCIフェイル条件導入](adr/ADR-0003-複雑度しきい値によるCIフェイル条件導入.md)
- [ADR-0004: コミットメッセージベースのPRサマリー自動コメント導入](adr/ADR-0004-コミットメッセージベースのPRサマリー自動コメント導入.md)
- [ADR-0005: PR本文への変更内容自動反映方式への変更](adr/ADR-0005-PR本文への変更内容自動反映方式への変更.md)
- [ADR-0006: テーブル設計共通方針](adr/ADR-0006-テーブル設計共通方針.md)
- [ADR-0007: 星取表マスタと経歴書のデータ連携方針](adr/ADR-0007-星取表マスタと経歴書のデータ連携方針.md)
- [ADR-0008: 経歴書のマスク範囲・コンタクト経路・ファイル出力範囲のスコープ判断](adr/ADR-0008-経歴書のマスク範囲-コンタクト経路-ファイル出力範囲のスコープ判断.md)
- [ADR-0009: 永続化技術スタックの導入（Flyway / MyBatis / PostgreSQL）](adr/ADR-0009-永続化技術スタックの導入-Flyway-MyBatis-PostgreSQL.md)

## Conventions

- [Git Prefixes](conventions/git-prefixes.md)
- [Pre-push Test Check](conventions/pre-push-test-check.md)
- [Pre-commit Secret Check](conventions/pre-commit-secret-check.md)
- [PR Summary 自動生成](conventions/pr-summary-automation.md)
- [Markdown見出しアンカーへのリンク規約](conventions/markdown-anchor-links.md)

## ADR 作成方法

ADR の作成・更新・Supersede は **ADR Governance エージェント** が支援します。
ClaudeCodeでは `/adr-governance` スキル（または `adr-governance` サブエージェント）、
Copilotでは `@ADR Governance` を呼び出してください。

- テンプレートと命名規則: [docs/adr/README.md](adr/README.md)
- 次の ADR 番号: **0010**

---

## Troubleshooting

- [Dev Container Compose Compatibility](troubleshooting/devcontainer-compose-compatibility.md)
- [Dev Container ClaudeCode Extension Missing](troubleshooting/devcontainer-claude-code-extension-missing.md)
- [Dev Container SSH Agent Forwarding](troubleshooting/devcontainer-ssh-agent-forwarding.md)
- [Gradle Java Home Invalid Folder](troubleshooting/gradle-javahome-invalid-folder.md)
- [Gradle Wrapper Lock Contention](troubleshooting/gradle-wrapper-lock-contention.md)

---

## Agents（マルチエージェント構成）

- [Agents概要・使い方（人間向け）](agents/README.md)
- [ナビゲーション指標ログ](agents/navigation-metrics.md) - 目次（AGENTS.md/docs/README.md）の機能度を振り返るログ
- [マルチエージェント構成 実行計画（exec-plan 0001）](exec-plans/active/0001-requirements-definition-multiagent.md)
- [旧マルチエージェント構成セットアップガイド（移行済み・リダイレクト）](agents/multi-agent-setup-guide.md)

全体マップ（AIエージェント向け索引）は [AGENTS.md](../AGENTS.md) を参照。

## Exec Plans / Design Docs

- [exec-plans運用ルール](exec-plans/README.md) - 実行計画（進行中/完了/技術的負債）
- [design-docs/index.md](design-docs/index.md) - design-docs索引
- [design-docs/core-beliefs.md](design-docs/core-beliefs.md) - 運用原則・思想

## Requirements（要件定義）

- [requirements/README.md](requirements/README.md) - プロジェクトコンテキスト・アーキテクチャ方針
- [requirements/ui-flows.md](requirements/ui-flows.md) - ユースケース・画面遷移（レビュー完了・要件定義確定）
- [requirements/data-models.md](requirements/data-models.md) - データモデル設計（レビュー完了・要件定義確定）
- [requirements/quality-standards.md](requirements/quality-standards.md) - 非機能要件・品質目標（レビュー完了・要件定義確定）

## Architecture（モジュール構成・パッケージ設計）

- [architecture/README.md](architecture/README.md) - モジュール構成・パッケージ設計・サブプロジェクト構成（実装フェーズで具体化予定）
