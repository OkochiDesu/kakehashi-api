# 0005: CI 統合テスト環境（GitHub Actions × Flyway × Testcontainers）

## 完了条件（Definition of Done）

- GitHub Actions の CI で PostgreSQL コンテナを起動し、Flyway マイグレーションが自動検証される
- `AccountRepositoryImplIntegrationTest` が CI で通過する

## 目的・スコープ

現状の `ci.yml` は単体テストのみ。PostgreSQL サービスコンテナを追加し、Flyway マイグレーションと統合テストを CI で実行できるようにする。

> **注意**: `.github/workflows/` の変更は CLAUDE.md のルールに従い、**着手前にユーザー確認必須**。

## 進捗状況

- [ ] `ci.yml` に PostgreSQL サービスコンテナ（`postgres:16`）を追加
- [ ] Flyway マイグレーション実行ステップを追加（`./gradlew flywayMigrate` または統合テストで代替）
- [ ] Testcontainers 統合テスト（`AccountRepositoryImplIntegrationTest`）が CI で通過することを確認
- [ ] PR 作成・マージ

> 根拠: [APP-ADR-0012](../../adr/APP-ADR-0012-Testcontainersを2.0.5へ移行しTestConfiguration直接起動方式を採用.md)

## 意思決定ログ

## 残課題・引き継ぎ事項
