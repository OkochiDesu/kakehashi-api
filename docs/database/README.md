# テーブルカタログ

このファイルは実装済みテーブルの一覧です。
テーブルの検索起点として使い、詳細は各リンク先を参照してください。

- **テーブル定義（DDL）の正**: `src/main/resources/db/migration/` 配下のFlywayマイグレーションSQL
- **設計レベルのデータモデル**: [docs/requirements/data-models.md](../requirements/data-models.md)

## テーブル一覧

| テーブル名 | 説明 | データモデル | 関連UC | DDL | 関連ADR |
|---|---|---|---|---|---|
| `accounts` | アカウント（社員コード・Google認証情報・ステータス管理） | [1章](../requirements/data-models.md#1-アカウントロールコンテキスト) | [UC-A1〜A6](../requirements/ui-flows.md#1-アカウントコンテキスト) | [V1](../../src/main/resources/db/migration/V1__create_accounts_and_roles.sql) | [APP-ADR-0001](../adr/APP-ADR-0001-テーブル設計共通方針.md), [APP-ADR-0004](../adr/APP-ADR-0004-永続化技術スタックの導入-Flyway-MyBatis-PostgreSQL.md) |
| `roles` | 権限マスタ（admin / view_personal_info） | [1章](../requirements/data-models.md#1-アカウントロールコンテキスト) | [UC-A6](../requirements/ui-flows.md#1-アカウントコンテキスト) | [V1](../../src/main/resources/db/migration/V1__create_accounts_and_roles.sql) | [APP-ADR-0001](../adr/APP-ADR-0001-テーブル設計共通方針.md), [APP-ADR-0004](../adr/APP-ADR-0004-永続化技術スタックの導入-Flyway-MyBatis-PostgreSQL.md), [APP-ADR-0007](../adr/APP-ADR-0007-rolesをpermissionベースに再定義しvisibility_rulesを廃止.md) |
| `account_roles` | アカウントと権限の紐付け（多対多） | [1章](../requirements/data-models.md#1-アカウントロールコンテキスト) | [UC-A6](../requirements/ui-flows.md#1-アカウントコンテキスト) | [V1](../../src/main/resources/db/migration/V1__create_accounts_and_roles.sql) | [APP-ADR-0001](../adr/APP-ADR-0001-テーブル設計共通方針.md), [APP-ADR-0004](../adr/APP-ADR-0004-永続化技術スタックの導入-Flyway-MyBatis-PostgreSQL.md), [APP-ADR-0007](../adr/APP-ADR-0007-rolesをpermissionベースに再定義しvisibility_rulesを廃止.md) |

## 運用ルール

- 新しいマイグレーション（`V2__...sql` 等）を追加した際は、このファイルに行を追記する
- DDLの変更が伴う場合は関連ADRを確認し、必要であれば `adr-governance` でADRを更新・追加する
- カラム定義やデータ型の詳細はDDLを参照（ここには転記しない）
