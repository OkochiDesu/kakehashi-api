# Docs Index

このディレクトリのドキュメント導線です。

## まず見る

- [TODO](TODO.md)
- [ADR一覧](adr/ADR-0001-CI品質ゲートとDependabot運用方針.md)

## ADR

- [ADR-0001: CI品質ゲートとDependabot運用方針](adr/ADR-0001-CI品質ゲートとDependabot運用方針.md)
- [ADR-0002: CIトリガー分離とWorkflow検証運用方針](adr/ADR-0002-CIトリガー分離とWorkflow検証運用方針.md)
- [ADR-0003: 複雑度しきい値によるCIフェイル条件導入](adr/ADR-0003-複雑度しきい値によるCIフェイル条件導入.md)
- [ADR-0004: コミットメッセージベースのPRサマリー自動コメント導入](adr/ADR-0004-コミットメッセージベースのPRサマリー自動コメント導入.md)

## Conventions

- [Git Prefixes](conventions/git-prefixes.md)
- [Pre-push Test Check](conventions/pre-push-test-check.md)
- [Pre-commit Secret Check](conventions/pre-commit-secret-check.md)
- [PR Summary 自動生成](conventions/pr-summary-automation.md)

## ADR 作成方法

ADR の作成・更新・Supersede は **ADR Governance エージェント** が支援します。
ClaudeCodeでは `/adr-governance` スキル（または `adr-governance` サブエージェント）、
Copilotでは `@ADR Governance` を呼び出してください。

- テンプレートと命名規則: [docs/adr/README.md](adr/README.md)
- 次の ADR 番号: **0006**

---

## Troubleshooting

- [Dev Container Compose Compatibility](troubleshooting/devcontainer-compose-compatibility.md)
- [Dev Container ClaudeCode Extension Missing](troubleshooting/devcontainer-claude-code-extension-missing.md)
- [Gradle Java Home Invalid Folder](troubleshooting/gradle-javahome-invalid-folder.md)
- [Gradle Wrapper Lock Contention](troubleshooting/gradle-wrapper-lock-contention.md)

---

## Agents（マルチエージェント構成）

- [Agents概要・使い方（人間向け）](agents/README.md)
- [マルチエージェント構成 実行計画（exec-plan 0001）](exec-plans/active/0001-requirements-definition-multiagent.md)
- [参考: Harness Engineering（OpenAI記事）](agents/openAI_harness_enjineerring.md)
- [旧マルチエージェント構成セットアップガイド（移行済み・リダイレクト）](agents/multi-agent-setup-guide.md)

全体マップ（AIエージェント向け索引）は [AGENTS.md](../AGENTS.md) を参照。

## Exec Plans / Design Docs

- [exec-plans運用ルール](exec-plans/README.md) - 実行計画（進行中/完了/技術的負債）
- [design-docs/index.md](design-docs/index.md) - design-docs索引
- [design-docs/core-beliefs.md](design-docs/core-beliefs.md) - 運用原則・思想
