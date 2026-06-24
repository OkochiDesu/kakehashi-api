# APP-ADR-0004: 永続化技術スタックの導入（Flyway / MyBatis / PostgreSQL）

## 目次

[ステータス](#ステータス) / [関連](#関連) / [背景](#背景) / [決定](#決定) / [代替案](#代替案) / [影響](#影響) / [今後の見直しポイント](#今後の見直しポイント)

## ステータス

- [ ] Proposed
- [x] Accepted
- [ ] Superseded
- [ ] Rejected

## 日付

2026-06-16

## 関連

- Supersedes: なし
- Superseded by: なし
- 補完: APP-ADR-0011（Testcontainers コアを 1.20.4 に固定、Superseded）/ [APP-ADR-0012](APP-ADR-0012-Testcontainersを2.0.5へ移行しTestConfiguration直接起動方式を採用.md)（Testcontainers 2.0.5 移行・確定パターン）— 本 ADR の見直しポイント「DB統合テスト導入時の依存・CI構成変更を記録する」を受けて作成された ADR 群。最新の方針は APP-ADR-0012 を参照。

## 背景

要件定義フェーズでデータベース・マイグレーション・O/Rマッパーの方針は「導入予定」として記述されていたが、ビルド依存・接続設定・マイグレーション配置といった実際のセットアップは未着手だった（[docs/requirements/README.md 技術スタック表](../requirements/README.md)）。

Step1実装フェーズ（Flywayマイグレーション → API設計 → Kotlin実装 → レビュー）を開始するにあたり（[exec-plan 0001](../exec-plans/active/0001-requirements-definition-multiagent.md)）、db-designerがFlywayマイグレーションSQL（`V*.sql`）を生成できる土台が必要になった。これに伴い、`build.gradle.kts` に永続化関連の依存を追加し、`application.properties` にデータソース・Flyway・MyBatisの設定を追加した。

技術選定自体は要件定義で既に方針として明示されている。

- データベース: PostgreSQL 16（JSONBを積極利用）。DB製品の置き換えは想定しない（[quality-standards.md 6章](../requirements/quality-standards.md)）。
- マイグレーション: Flyway（[requirements/README.md 技術スタック表](../requirements/README.md)）。
- O/Rマッパー: MyBatis。MyBatis以外のO/Rマッパーへの変更等、将来の技術選定変更はADRで判断する（[quality-standards.md 6章](../requirements/quality-standards.md)）。
- アーキテクチャ: CQRSにより更新系（ドメインモデル経由）と参照系（MyBatis直結でJSONB/JOIN結果をDTOに直接マッピング）を分離する（[data-models.md 1章](../requirements/data-models.md)、[requirements/README.md 4章](../requirements/README.md)）。

本ADRは、これらの確定済み方針を実装上のセットアップとして反映した事実と、その構成の根拠を記録する。

## 決定

永続化技術スタックを以下の構成で導入する。

1. **依存（`build.gradle.kts`）**: `spring-boot-starter-jdbc`、`flyway-core`、`flyway-database-postgresql`、`postgresql`（runtimeOnly）、`mybatis-spring-boot-starter:3.0.4` を追加する。CI環境でのDB統合テスト用Testcontainersは必要になった時点で追加する（現時点では追加しない）。
2. **データソース（`application.properties`）**: devcontainerの docker-compose の `db` サービス（`jdbc:postgresql://db:5432/...`）へ接続する。接続情報（DB名・ユーザー・パスワード）は環境変数（`POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD`）から注入し、未設定時のデフォルト値をローカル開発用に持たせる。
3. **Flyway**: `spring.flyway.enabled=true`、配置先は `classpath:db/migration`（`src/main/resources/db/migration/`）とする。db-designerが生成する `V*.sql` をここに配置する。
4. **MyBatis**: マッパーXMLの配置先を `classpath:mapper/**/*.xml`（`src/main/resources/mapper/`）とし、`map-underscore-to-camel-case=true` でスネークケース列名をキャメルケースへ自動マッピングする。

対象ファイル:

- [build.gradle.kts](../../build.gradle.kts)
- [src/main/resources/application.properties](../../src/main/resources/application.properties)
- `src/main/resources/db/migration/`（Flywayマイグレーション配置先）
- `src/main/resources/mapper/`（MyBatisマッパーXML配置先）

## 代替案

### 代替案A: O/RマッパーにJPA/Hibernateを採用する

- 長所: エンティティ駆動でCRUDの定型コードを削減でき、Spring Data JPAのエコシステムが厚い。
- 短所: 要件のCQRS方針では参照系でJSONB/JOIN結果をDTOへ直接マッピングして高速に返すことを重視しており、SQLを明示的に制御できるMyBatisの方が適合する。要件定義で既にMyBatisを方針として確定済みのため、本ADRではこれを踏襲した（[quality-standards.md 6章](../requirements/quality-standards.md)）。

### 代替案B: スキーマ管理をマイグレーションツールなし（手動DDL / Hibernate ddl-auto）で運用する

- 長所: 初期セットアップが簡単。
- 短所: スキーマ変更の履歴・再現性が担保されず、環境間差異やレビュー困難を招く。要件でFlywayを方針として確定済みであり、バージョン管理されたマイグレーションを採用した。

### 代替案C: 導入時点でTestcontainersによるDB統合テストを併せて整備する

- 長所: 早期にマイグレーション・マッパーの結合検証が可能になる。
- 短所: CIでのコンテナ実行環境・実行時間のコストが発生する。現段階ではマイグレーションSQLの作成が先行タスクであり、統合テストが必要になった時点で追加する判断とした（`build.gradle.kts` のコメントに明記）。

## 影響

- 以降のマイグレーションSQLは `src/main/resources/db/migration/` に `V*.sql` 命名で配置する前提となる（db-designerの出力先）。
- マッパーXMLは `src/main/resources/mapper/` 配下に配置すれば自動的にロードされる。
- データソース接続はdevcontainerの `db` サービスを前提とするため、別環境（CI・本番）での接続情報は環境変数で上書きする運用になる。クラウド事業者・マネージドPostgreSQL等のホスティングは未確定であり、確定時にADRへ記録する（[quality-standards.md 6章](../requirements/quality-standards.md)、[docs/TODO.md](../TODO.md)）。
- `accounts.account_id` の `AZ0000` 確定フォーマットやUUID v7生成方式は本ADRの対象外であり、ADR-0006の見直しポイントに従い別途確定する。
- Spotlessのマッパー XML / SQL フォーマット設定は、最初のマッパー作成時に追加予定（`build.gradle.kts` のコメントに明記）。

## 今後の見直しポイント

- O/Rマッパー（MyBatis）やマイグレーションツール（Flyway）を変更する場合は、新規ADRで本ADRをSupersedeする。
- DB統合テスト（Testcontainers等）の依存・CI構成変更は [APP-ADR-0012](APP-ADR-0012-Testcontainersを2.0.5へ移行しTestConfiguration直接起動方式を採用.md) に記録済み（APP-ADR-0011 を Supersede）。
- クラウド事業者・マネージドPostgreSQL等のホスティングが確定した際は、データソース設定の運用方針をADRに記録する。
- MyBatisのマッパーXML / SQLに対するSpotlessフォーマット設定を追加した際は、CI品質ゲート関連の方針（CICD-ADR-0001〜0003系）との整合を確認する。
