# 0005: CI 統合テスト環境（GitHub Actions × Flyway × Testcontainers）

## 完了条件（Definition of Done）

- GitHub Actions の CI で PostgreSQL コンテナを起動し、Flyway マイグレーションが自動検証される
- `AccountRepositoryImplIntegrationTest` が CI で通過する

## 目的・スコープ

現状の `ci.yml` は単体テストのみ。PostgreSQL サービスコンテナを追加し、Flyway マイグレーションと統合テストを CI で実行できるようにする。

> **注意**: `.github/workflows/` の変更は CLAUDE.md のルールに従い、**着手前にユーザー確認必須**。

## 進捗状況

- [x] `ci.yml` に PostgreSQL サービスコンテナ（`postgres:16`）を追加 → **不要と判明**（下記参照）
- [x] Flyway マイグレーション実行ステップを追加 → **不要と判明**（下記参照）
- [x] Testcontainers 統合テスト（`AccountRepositoryImplIntegrationTest`）が CI で通過することを確認 → 既存 `ci.yml` のままで確認済み
- [x] PR 作成・マージ → 対象外（`.github/workflows/` 変更なしのため）

> 根拠: [APP-ADR-0012](../../adr/APP-ADR-0012-Testcontainersを2.0.5へ移行しTestConfiguration直接起動方式を採用.md)（→ [APP-ADR-0013](../../adr/APP-ADR-0013-Testcontainers統合テストをServiceConnection方式へ移行.md) へ Supersede 済み）

## 意思決定ログ

- 2026-06-30: 着手前調査で、本 exec-plan の前提（GitHub Actions に明示的な PostgreSQL サービスコンテナが必要）が現状と乖離していると判明。APP-ADR-0013（`@ServiceConnection` 方式への移行）により、`AccountRepositoryImplIntegrationTest` は Testcontainers が GitHub Actions ランナーの native Docker を使って自前で PostgreSQL コンテナを起動・破棄する。GitHub Actions の `services:` ブロックは不要
- 2026-06-30: `gh run view 28310776681 --log`（`main` ブランチ・コミット `0f9ee16` の CI run）で `AccountRepositoryImplIntegrationTest` の全テストケース（Flyway マイグレーション検証含む）が PASSED していることを確認。既存 `ci.yml`（`.github/workflows/` 変更なし）のままで本 exec-plan の DoD は達成済みと判断し、`.github/workflows/` への変更は行わずクローズする

## 残課題・引き継ぎ事項

- なし（`.github/workflows/` の変更は不要だったため、CLAUDE.md の「CIフック・ワークフロー変更時の確認」手続きも対象外）
