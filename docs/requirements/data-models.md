# data-models.md（データモデル設計）

> ステータス: ドラフト完成（全6コンテキスト記載済み・経歴書の正規化テーブル化・検索条件対応反映済み・レビュー中）

RDBの厳密なカラムと、JSONBで柔軟に持つべきデータの境界を整理する。

前提となるプロジェクトコンテキストは [README.md](README.md) を参照。コンテキストの区切りは [ui-flows.md](ui-flows.md) と対応する。

## 0. 設計方針

> 本書は**永続化層（RDB/JSONB）のスキーマ設計**を対象とする。ドメインモデル（エンティティ・値オブジェクト・集約の振る舞いや不変条件）は、イベントストーミングで識別済みの集約を出発点としつつ、実装フェーズで別途設計する。本書のテーブル構成はその「たたき台」であり、ドメインモデルの集約境界と1:1にならない場合がある。

- **RDB厳密カラム**: 検索・フィルタ・一意制約・外部キー制約が必要な項目（ID、コード、状態、区分、日時等）は、正規化したテーブルのカラムとして持つ。Step1の経歴書・星取表は、検索条件（[quality-standards.md 1章](quality-standards.md#1-機能適合性functional-suitability)）に対応するため正規化テーブルで持つ。
- **JSONB柔軟カラム**: フォーマットが年次更新され、構造自体が大きく変わるデータ（キャリアシート等）に適用する方式。キャリアシートはStep1のスコープ外（[README.md 7章](README.md#7-イベントストーミング結果と業務ルール)）のため、Step1では本書に具体例はない。
- **CQRS**:
  - Command側: 集約はドメインモデルを経由し、正規化テーブル（必要に応じてJSONB）に永続化する。
  - Query側: ドメイン層をバイパスし、MyBatisでJOIN結果・JSONBを直接DTOにマッピングして画面へ返す。
- **編集系テーブルの共通カラム**: ユーザーが編集する主要テーブルには、楽観ロック用の`version`（integer、更新ごとにインクリメント）と、編集者記録用の`updated_by`（bigint, FK → accounts.id）を付与する（[quality-standards.md 1章・6章](quality-standards.md#1-機能適合性functional-suitability)）。
- **編集履歴ログ**: 上記テーブルの変更履歴は、汎用の`entity_change_logs`（id, entity_type, entity_id, account_id, action, changed_at, before, after）で記録する想定。対象テーブル・粒度の詳細は実装フェーズで確定する。
- 命名はテーブル名・カラム名ともsnake_case。各テーブルのPKは `id`（bigserial想定、確定はADRで行う）。
- 本章では各コンテキストの集約に対応するテーブルの**論理設計**（主なカラムと型の方向性）を整理する。物理設計（インデックス、制約の詳細等）はADR/マイグレーションファイルで確定する。

## 1. 認証・アカウントコンテキスト

[ui-flows.md 1章](ui-flows.md#1-認証アカウントコンテキスト)（UC-A1〜A7）に対応。

### accounts（RDB厳密カラム）

| カラム | 型 | 説明 |
|---|---|---|
| id | bigserial | PK |
| google_sub_hash | text (unique) | Google SSOの`sub`クレームの決定的ハッシュ（SHA-256等）。ログイン時はハッシュ化して比較し、平文の`sub`は保持しない |
| email | text | Googleアカウントのメールアドレス |
| name | text | 表示名（Googleプロフィールの`name`をそのまま保持） |
| status | text/enum | アカウント状態（仮登録／本登録／停止） |
| suspended_at | timestamp (nullable) | 停止開始日時。「停止から1年経過」の判定に使用（[quality-standards.md 1章](quality-standards.md#1-機能適合性functional-suitability)） |
| version | integer | 楽観ロック用バージョン |
| updated_by | bigint (FK → accounts.id, nullable) | 最終更新者（権限変更・停止操作を行った管理者等） |
| created_at | timestamp | 仮登録日時 |
| updated_at | timestamp | 最終更新日時 |

### roles（ロール・区分マスタ、RDB厳密カラム）

| カラム | 型 | 説明 |
|---|---|---|
| id | bigserial | PK |
| code | text (unique) | ロールコード（例: `general`, `admin`） |
| name | text | ロール名 |
| created_at / updated_at | timestamp | 作成・更新日時 |

### account_roles（アカウント×ロール、RDB厳密カラム）

| カラム | 型 | 説明 |
|---|---|---|
| id | bigserial | PK |
| account_id | bigint (FK → accounts.id) | 対象アカウント |
| role_id | bigint (FK → roles.id) | 付与されたロール |
| created_at | timestamp | 付与日時 |

### visibility_rules（ロール別の項目可視性ルール、RDB厳密カラム）

| カラム | 型 | 説明 |
|---|---|---|
| id | bigserial | PK |
| role_id | bigint (FK → roles.id) | 対象ロール |
| target_category | text/enum | 可視性を制御する対象区分（例: 経歴書の個人情報項目区分） |
| can_view | boolean | 当該ロールが対象区分を閲覧可能か |

### 補足

- 「仮登録」「本登録（自動処理）」は`accounts.status`の状態遷移として表現する（[ui-flows.md 1章の補足](ui-flows.md#補足)で説明した内部処理・自動処理を含む）。
- 「停止／停止解除」（UC-A7）は`accounts.status`の状態遷移として表現し、停止時に`suspended_at`を設定、解除時に`suspended_at`をNULLに戻す。
- 「権限変更」（UC-A6）は、`accounts.role`という単一カラムではなく、`account_roles`（アカウント×ロールの多対多）の行の追加・削除として表現する。1アカウントが複数ロールを持つことを許容する。
- **長期停止アカウントのマスク化**: `suspended_at`から1年以上経過したアカウントは、エンジニアページ等での表示がマスク対象となる（[quality-standards.md 1章](quality-standards.md#1-機能適合性functional-suitability)）。バッチでステータスを更新するか、参照時に`suspended_at`から動的判定するかは実装フェーズで決定する。
- `roles` / `visibility_rules`は、4章「経歴書コンテキスト」のマスク済み経歴書・星取表閲覧（UC-R2/UC-S5）の可視性制御と直結する。「どの区分の項目を、どのロールに見せるか」を`visibility_rules`で表現する。`target_category`は経歴書（`resumes`/`resume_projects`の列単位）・星取表（`skill_categories`/`level_categories`単位）の両方を対象とする想定。
- 会社ドメインチェック（CSVメモ「会社のドメイン（環境変数）をチェックしたい」）はアプリケーション設定（環境変数）で行うため、テーブル設計には影響しない。
- 経歴書・星取表など他コンテキストのテーブルは`accounts.id`を外部キーとして参照する。

## 2. メッセージコンテキスト

[ui-flows.md 2章](ui-flows.md#2-メッセージコンテキスト)（UC-M1）に対応。

### 補足

- UC-M1（エンジニアへのコンタクト）は、Googleメッセージ（外部サービス）を起点とするコンタクトであり、現時点では本リポジトリ側で永続化するデータモデルは想定しない。
- **未確定事項**: 営業担当を介する運用（[ui-flows.md 2章の補足](ui-flows.md#補足-1)）が確定した場合、コンタクト履歴やコンタクト先（営業担当）の紐付けをRDBで管理する必要が出る可能性がある。その場合は`contacts`テーブル等を追加検討する。

## 3. 星取表コンテキスト

[ui-flows.md 3章](ui-flows.md#3-星取表コンテキスト)（UC-S1〜S5）に対応。

### skill_categories（スキル区分マスタ、RDB厳密カラム）

| カラム | 型 | 説明 |
|---|---|---|
| id | bigserial | PK |
| code | text (unique) | 区分コード（例: `language`, `framework`, `cloud`） |
| name | text | 区分名 |
| display_order | integer | 区分の表示順（疎な整数、10/100刻み等で運用） |
| created_at / updated_at | timestamp | 作成・更新日時 |

### skill_master_items（星取表マスタ／スキル項目、RDB厳密カラム）

| カラム | 型 | 説明 |
|---|---|---|
| id | bigserial | PK |
| code | text (unique) | スキルコード（`skill_XX`形式、プレフィックスで項目種別を一意に管理） |
| name | text | スキル項目名 |
| category_id | bigint (FK → skill_categories.id) | 区分（言語／フレームワーク／クラウド等） |
| status | text/enum | 状態（有効／アーカイブ） |
| display_order | integer | 区分内での表示順（疎な整数、10/100刻み等で運用） |
| version | integer | 楽観ロック用バージョン |
| updated_by | bigint (FK → accounts.id) | 最終更新者（管理者） |
| created_at / updated_at | timestamp | 作成・更新日時 |

### level_categories（レベル区分マスタ、RDB厳密カラム）

| カラム | 型 | 説明 |
|---|---|---|
| id | bigserial | PK |
| code | text (unique) | 区分コード（例: `infra`, `application`） |
| name | text | 区分名 |
| display_order | integer | 区分の表示順（疎な整数、10/100刻み等で運用） |
| created_at / updated_at | timestamp | 作成・更新日時 |

### level_master_items（星取表マスタ／レベル項目、RDB厳密カラム）

| カラム | 型 | 説明 |
|---|---|---|
| id | bigserial | PK |
| code | text (unique) | レベルコード（`level_xx`形式） |
| name | text | レベル項目名（例: 初級／中級／上級など） |
| description | text | レベルの説明 |
| category_id | bigint (FK → level_categories.id) | 区分（インフラ／アプリケーション等、レベル定義が領域によって異なることに対応） |
| status | text/enum | 状態（有効／アーカイブ） |
| display_order | integer | 区分内での表示順（疎な整数、10/100刻み等で運用） |
| version | integer | 楽観ロック用バージョン |
| updated_by | bigint (FK → accounts.id) | 最終更新者（管理者） |
| created_at / updated_at | timestamp | 作成・更新日時 |

### user_skills（星取表／スキル、RDB厳密カラム）

| カラム | 型 | 説明 |
|---|---|---|
| id | bigserial | PK |
| account_id | bigint (FK → accounts.id) | 所有者 |
| skill_master_item_id | bigint (FK → skill_master_items.id) | 登録したスキル項目 |
| version | integer | 楽観ロック用バージョン |
| updated_by | bigint (FK → accounts.id) | 最終更新者（本人） |
| created_at | timestamp | 登録日時 |

### user_skill_levels（星取表／レベル、RDB厳密カラム）

| カラム | 型 | 説明 |
|---|---|---|
| id | bigserial | PK |
| account_id | bigint (FK → accounts.id) | 所有者 |
| skill_master_item_id | bigint (FK → skill_master_items.id) | 対象スキル項目 |
| level_master_item_id | bigint (FK → level_master_items.id) | 設定したレベル項目 |
| version | integer | 楽観ロック用バージョン |
| updated_by | bigint (FK → accounts.id) | 最終更新者（本人） |
| updated_at | timestamp | 更新日時 |

### 補足

- 「区分」は`skill_categories` / `level_categories`という専用マスタテーブルとして切り出し、`skill_master_items` / `level_master_items`から`category_id`で参照する（[ui-flows.md 3章の補足](ui-flows.md#補足-2)で確認した方針）。区分自体の並び替えは各categoriesテーブルの`display_order`、区分内の項目の並び替えは各master_itemsテーブルの`display_order`で行い、それぞれ疎な整数（10/100刻み等）で運用することで並び替え時の更新範囲を抑える。検索ページの「星取表（スキル×レベル）」条件や、エンジニアページ・マイページでのグルーピング表示・レイアウト切替（[ui-flows.md 3章の補足](ui-flows.md#補足-2)のUC-S5）に使用する想定。
- `level_categories`（インフラ／アプリケーション等）により、レベル定義が領域ごとに異なる場合に対応する。
- スキルコード／レベルコードは `skill_XX` / `level_xx` のように接頭辞でカテゴリを一意に管理する（CSVメモ「VOでスキルコード・レベルコードをチェックする」方針）。一意性のルール自体はDB制約では強制せず、コード発行・検証はアプリケーション側（値オブジェクト）で行う。
- `user_skills`（スキルの保有登録）と`user_skill_levels`（スキルへのレベル設定）は、イベントストーミング上の登録・更新・削除イベントの粒度に合わせて別テーブルとした。**未確定事項**: 実装時に1テーブルへ統合するか（`user_skills`に`level_master_item_id`をnullable列として持たせるか）は、ドメインモデル設計（集約境界）と合わせて再検討する。
- 一括登録系（星取表マスタ一括登録、星取表一括登録（星取表CSV））は[README.md 7章](README.md#7-イベントストーミング結果と業務ルール)の方針通りAPI化せず、上記テーブルへのSQL直接投入で対応する。
- マスタのアーカイブ／アーカイブ解除は`skill_master_items.status` / `level_master_items.status`の状態遷移で表現する。
- `user_skills` / `user_skill_levels`への`version` / `updated_by`の付与は、[quality-standards.md 1章・6章](quality-standards.md#1-機能適合性functional-suitability)で確定した「編集系テーブルの共通カラム」方針（0章）に基づく。

## 4. 経歴書コンテキスト

[ui-flows.md 4章](ui-flows.md#4-経歴書コンテキスト)（UC-R1〜R4）に対応。

> 経歴書は「案件名×言語」での検索（[quality-standards.md 1章](quality-standards.md#1-機能適合性functional-suitability)）に対応するため、`resumes.content`へのJSONB集約ではなく、正規化テーブル（`resumes` / `resume_qualifications` / `resume_projects`）で構成する。

### resumes（経歴書本体・基本情報、RDB厳密カラム）

| カラム | 型 | 説明 |
|---|---|---|
| id | bigserial | PK |
| account_id | bigint (FK → accounts.id, unique) | 所有者（本人）。1アカウントにつき経歴書1件 |
| age | integer | 年齢 |
| nearest_station | text | 最寄り駅 |
| final_education | text | 最終学歴 |
| self_pr | text | 自己PR |
| status | text/enum | 状態（有効／アーカイブ） |
| version | integer | 楽観ロック用バージョン |
| updated_by | bigint (FK → accounts.id) | 最終更新者（本人） |
| created_at / updated_at | timestamp | 登録・更新日時 |

### resume_qualifications（経歴書／資格情報、RDB厳密カラム）

| カラム | 型 | 説明 |
|---|---|---|
| id | bigserial | PK |
| resume_id | bigint (FK → resumes.id) | 所属する経歴書 |
| name | text | 資格名 |
| acquired_date | date (nullable) | 取得年月 |
| display_order | integer | 表示順（疎な整数、10/100刻み等で運用） |
| created_at / updated_at | timestamp | 作成・更新日時 |

### resume_projects（経歴書／案件経歴、RDB厳密カラム）

| カラム | 型 | 説明 |
|---|---|---|
| id | bigserial | PK |
| resume_id | bigint (FK → resumes.id) | 所属する経歴書 |
| period_start | date | 案件期間（開始） |
| period_end | date (nullable) | 案件期間（終了、進行中はNULL） |
| project_name | text | 案件名 |
| description | text | 業務内容 |
| os_db | text[] | OS／DB（複数指定可） |
| languages_tools | text[] | 言語・ツール・FW（複数指定可。GINインデックスを付与し検索に使用） |
| position | text | ポジション |
| team_size | integer | 人数 |
| process_phases | text[] | 工程（要件定義／設計／実装／テスト等、複数指定可） |
| display_order | integer | 表示順（疎な整数、10/100刻み等で運用） |
| created_at / updated_at | timestamp | 作成・更新日時 |

### 補足

- `resumes`を基本情報（旧テーブルA）と集約（旧テーブルD）を統合したテーブル、`resume_qualifications`を資格情報（旧テーブルB）、`resume_projects`を案件経歴（旧テーブルC）として正規化する。`resume_qualifications` / `resume_projects`は`resumes`の子テーブルであり、経歴書全体の編集（UC-R1）はトランザクション内で`resumes.version`を用いて楽観ロックする想定（子テーブル自体には`version`/`updated_by`を持たせない）。
- 経歴書検索（「案件名×言語」、単体検索可）は`resumes`と`resume_projects`をJOINし、`resume_projects.project_name`（LIKE等）と`resume_projects.languages_tools`（`text[]`への`&&`/`@>`演算子）を条件に検索する。`languages_tools`にはGINインデックスを付与する。
- 経歴書のアーカイブ／アーカイブ解除（UC-R3）は`resumes.status`の状態遷移で表現する。
- 一括登録（経歴書CSV）は[README.md 7章](README.md#7-イベントストーミング結果と業務ルール)の方針通りSQL直接投入で対応する。
- **フォーマット変更への対応**: キャリアシートと異なり、経歴書のフォーマット変更（項目の追加等）はJSONBスキーマ管理ではなく、テーブルへのカラム追加マイグレーションで対応する（[README.md 7章](README.md#7-イベントストーミング結果と業務ルール)で経歴書とキャリアシートのバージョニング要件を分離）。
- **未確定事項**: 「（検索された）マスク有済み経歴書」（本人以外が閲覧する際のマスク対象項目）をどう実現するか。1章の`visibility_rules.target_category`を`resumes` / `resume_projects`のどの列単位で適用するか（カラム名ベースのマッピング等）は、UC-R2のQuery実装時に詳細化する。
- **未確定事項**: 「経歴書を更新するタイミングで星取表にスキルを反映できないか？」は、`resume_projects.languages_tools`と`user_skills`（3章）の連携方針が未確定のため、Step1での対応範囲は今後判断する。
- 経歴書項目検索・一覧（UC-R4、管理者画面）はQuery側で`resumes` / `resume_projects`をJOINしてMyBatisで直接マッピングして返す想定。

## 5. ファイルコンテキスト

[ui-flows.md 5章](ui-flows.md#5-ファイルコンテキスト)（UC-F1）に対応。

### 補足

- UC-F1（経歴書ファイル出力）は、`resumes` / `resume_qualifications` / `resume_projects`（4章）を基にExcel/PDFを動的に生成する処理であり、出力ファイル自体を永続化するデータモデルは現時点では想定しない（[README.md 4章](README.md#4-アーキテクチャ原則adr化予定の方針)のExcel正・PDF変換方針）。経歴書はJSONBスキーマ管理ではないため、フォーマットバージョン（キャリアシート4章想定）の概念は経歴書出力には適用しない。
- **未確定事項**: 出力履歴（誰がいつ誰の経歴書を出力したか）を記録する要件が出た場合は、`resume_export_logs`（account_id, target_account_id, exported_at等）のようなテーブルを追加検討する。
