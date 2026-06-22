# data-models.md（データモデル設計）

> ステータス: レビュー完了・要件定義確定（Step1範囲）（全6コンテキスト記載済み・経歴書の正規化テーブル化・検索条件対応反映済み）

RDBの厳密なカラムと、JSONBで柔軟に持つべきデータの境界を整理する。

前提となるプロジェクトコンテキストは [README.md](README.md) を参照。コンテキストの区切りは [ui-flows.md](ui-flows.md) と対応する。

## 目次

- [0. 設計方針](#0-設計方針)
- [1. 認証・アカウントコンテキスト](#1-認証アカウントコンテキスト)
- [2. メッセージコンテキスト](#2-メッセージコンテキスト)
- [3. 星取表コンテキスト](#3-星取表コンテキスト)
- [4. 経歴書コンテキスト](#4-経歴書コンテキスト)
- [5. ファイルコンテキスト](#5-ファイルコンテキスト)

## 0. 設計方針

> 本書は**永続化層（RDB/JSONB）のスキーマ設計**を対象とする。ドメインモデル（エンティティ・値オブジェクト・集約の振る舞いや不変条件）は、イベントストーミングで識別済みの集約を出発点としつつ、実装フェーズで別途設計する。本書のテーブル構成はその「たたき台」であり、ドメインモデルの集約境界と1:1にならない場合がある。

- **RDB厳密カラム**: 検索・フィルタ・一意制約・外部キー制約が必要な項目（ID、コード、状態、区分、日時等）は、正規化したテーブルのカラムとして持つ。Step1の経歴書・星取表は、検索条件（[quality-standards.md 1章](quality-standards.md#1-機能適合性functional-suitability)）に対応するため正規化テーブルで持つ。
- **JSONB柔軟カラム**: フォーマットが年次更新され、構造自体が大きく変わるデータ（キャリアシート等）に適用する方式。キャリアシートはStep1のスコープ外（[README.md 7章](README.md#7-イベントストーミング結果と業務ルール)）のため、Step1では本書に具体例はない。
- **CQRS**:
  - Command側: 集約はドメインモデルを経由し、正規化テーブル（必要に応じてJSONB）に永続化する。
  - Query側: ドメイン層をバイパスし、MyBatisでJOIN結果・JSONBを直接DTOにマッピングして画面へ返す。
- **監査カラム（全テーブル共通）**: 全テーブルに`created_at` / `updated_at`（timestamp）、`created_by` / `updated_by`（text）を付与する。`created_by` / `updated_by`には、ユーザー操作の場合は`accounts.account_id`（後述の文字列形式ID）、バッチ処理（SQL直接投入等）の場合は処理を識別するリクエストIDなどの文字列を格納する。値の種類が混在するためFK制約は持たせない。
- **編集系テーブルの楽観ロック**: ユーザーが編集する主要テーブル（`accounts` / `skill_master_items` / `level_master_items` / `user_skills` / `resumes`）には、楽観ロック用の`version`（integer、更新ごとにインクリメント）を付与する（[quality-standards.md 1章・6章](quality-standards.md#1-機能適合性functional-suitability)）。version方式を採用した理由は [APP-ADR-0005](../adr/APP-ADR-0005-楽観ロックにversionカラム整数カウンタを採用.md) を参照。
- **編集履歴ログ**: 上記テーブルの変更履歴は、汎用の`entity_change_logs`（id, entity_type, entity_id, created_by, action, changed_at, before, after）で記録する想定。対象テーブル・粒度の詳細は実装フェーズで確定する。
- **命名規則**: テーブル名・カラム名ともsnake_case。各テーブルのPKカラム名は`<エンティティ名（単数形）>_id`とする（例: `account_id`, `role_id`, `skill_category_id`）。`id`という単独カラム名は使わない。
- **PKの型・採番方式**:
  - `accounts.account_id`: text型。`AZ0000`のようなプレフィックス付き連番形式（仮フォーマット、確定はADRで行う）。社員コード相当の識別子として、アプリ側で採番する。
  - 上記以外のテーブルのPK: UUID（v7想定）。アプリ側で採番する。
- **display_orderの運用**: 区分・項目の表示順は1始まりの連番とする（疎な整数は使わない）。並び替え時は対象範囲内の複数行の`display_order`を更新する。
- 本章では各コンテキストの集約に対応するテーブルの**論理設計**（主なカラムと型の方向性）を整理する。物理設計（インデックス、制約の詳細等）はADR/マイグレーションファイルで確定する。

## 1. 認証・アカウントコンテキスト

[ui-flows.md 1章](ui-flows.md#1-認証アカウントコンテキスト)（UC-A1〜A7）に対応。

### accounts（RDB厳密カラム）

| カラム | 型 | 説明 |
|---|---|---|
| account_id | text | PK。`AZ0000`のようなプレフィックス付き連番形式（仮フォーマット）。社員コード相当の識別子で、アプリ側で採番する |
| google_sub_hash | text (unique) | Google SSOの`sub`クレームの決定的ハッシュ（SHA-256等）。ログイン時はハッシュ化して比較し、平文の`sub`は保持しない |
| email | text | Googleアカウントのメールアドレス |
| name | text | 表示名（Googleプロフィールの`name`をそのまま保持） |
| status | text/enum | アカウント状態。`provisional`（仮登録）/ `active`（本登録済み）/ `suspended`（停止中）/ `deactivated`（廃止）の4値。退職と一時停止は区別しない（[APP-ADR-0006](../adr/APP-ADR-0006-accounts.statusに4値設計（deactivated追加）と非adminからのsuspended-deactivated除外.md)） |
| suspended_at | timestamp (nullable) | 停止開始日時。停止解除時にNULLに戻す。`suspended_at`から1年経過後に`@Scheduled`日次タスクで`deactivated`へ自動遷移（[APP-ADR-0006](../adr/APP-ADR-0006-accounts.statusに4値設計（deactivated追加）と非adminからのsuspended-deactivated除外.md)） |
| version | integer | 楽観ロック用バージョン |
| created_by | text | 登録者（`accounts.account_id`またはバッチのリクエストID） |
| updated_by | text | 最終更新者（権限変更・停止操作を行った管理者の`accounts.account_id`等） |
| created_at | timestamp | 仮登録日時 |
| updated_at | timestamp | 最終更新日時 |

### roles（権限マスタ、RDB厳密カラム）

`roles` テーブルは「できること（Permission）」のマスタ（[APP-ADR-0007](../adr/APP-ADR-0007-rolesをpermissionベースに再定義しvisibility_rulesを廃止.md)）。Step1の初期データは以下の2件。

| `code` | `name` | 概要 |
|---|---|---|
| `admin` | 管理業務 | 権限付与・停止・復活・他ユーザー情報変更 |
| `view_personal_info` | 個人情報表示 | 他ユーザーの `nearest_station` / `final_education` 閲覧 |

| カラム | 型 | 説明 |
|---|---|---|
| role_id | uuid | PK（アプリ側で採番） |
| code | text (unique) | 権限コード（例: `admin`, `view_personal_info`） |
| name | text | 権限名 |
| created_by / updated_by | text | 登録者・最終更新者 |
| created_at / updated_at | timestamp | 作成・更新日時 |

### account_roles（アカウント×権限、RDB厳密カラム）

アカウントに付与された権限の集合。認可判定は「対象操作に必要な権限が `account_roles` に存在するか」で行う。

| カラム | 型 | 説明 |
|---|---|---|
| account_role_id | uuid | PK（アプリ側で採番） |
| account_id | text (FK → accounts.account_id) | 対象アカウント |
| role_id | uuid (FK → roles.role_id) | 付与された権限 |
| created_by / updated_by | text | 登録者・最終更新者 |
| created_at / updated_at | timestamp | 付与日時・更新日時 |

### 補足

- 「仮登録」「本登録（自動処理）」は`accounts.status`の状態遷移として表現する（[ui-flows.md 1章](ui-flows.md#1-認証アカウントコンテキスト)の補足で説明した内部処理・自動処理を含む）。
- 「停止／停止解除」（UC-A7）は`accounts.status`の状態遷移として表現し、停止時に`suspended_at`を設定、解除時に`suspended_at`をNULLに戻す。
- 「権限変更」（UC-A6）は`account_roles`の行追加・削除として表現する。本登録時（UC-A3）のデフォルト権限付与はなし。権限は管理者が明示的に付与する。
- **deactivatedへの自動遷移とマスク化**: `suspended_at`から1年経過したアカウントはSpring `@Scheduled`日次タスクで`status = 'deactivated'`に更新する。`deactivated`アカウントはAPIレスポンス時に`name`/`email`を常にマスク（`"***"`等）して返す（[APP-ADR-0006](../adr/APP-ADR-0006-accounts.statusに4値設計（deactivated追加）と非adminからのsuspended-deactivated除外.md)）。
- **ステータスごとの検索可視性**: status未指定時は権限に関わらず`active`のみ返す。`suspended`/`deactivated`は`admin`権限ありが明示指定した場合のみ返す（権限なしでは明示指定しても`active`に強制）（[APP-ADR-0006](../adr/APP-ADR-0006-accounts.statusに4値設計（deactivated追加）と非adminからのsuspended-deactivated除外.md)）。
- 経歴書の個人情報（`nearest_station`/`final_education`）のマスク判定は`view_personal_info`権限の有無で行う（[APP-ADR-0003決定1](../adr/APP-ADR-0003-経歴書のマスク範囲-コンタクト経路-ファイル出力範囲のスコープ判断.md)・[APP-ADR-0007](../adr/APP-ADR-0007-rolesをpermissionベースに再定義しvisibility_rulesを廃止.md)）。
- 会社ドメインチェック（CSVメモ「会社のドメイン（環境変数）をチェックしたい」）はアプリケーション設定（環境変数）で行うため、テーブル設計には影響しない。
- 経歴書・星取表など他コンテキストのテーブルは`accounts.account_id`を外部キーとして参照する。

## 2. メッセージコンテキスト

[ui-flows.md 2章](ui-flows.md#2-メッセージコンテキスト)（UC-M1）に対応。

### 補足

- UC-M1（エンジニアへのコンタクト）は、Googleメッセージ（外部サービス）を起点とするコンタクトであり、現時点では本リポジトリ側で永続化するデータモデルは想定しない。
- Step1では営業担当を介する運用は対象外（[ui-flows.md 2章](ui-flows.md#2-メッセージコンテキスト)の補足）のため、`contacts`テーブル等の追加は不要。営業経由ルーティングをStep2で導入する場合は、コンタクト履歴・コンタクト先（営業担当）の紐付けを管理するテーブルを別途検討する。

## 3. 星取表コンテキスト

[ui-flows.md 3章](ui-flows.md#3-星取表コンテキスト)（UC-S1〜S5）に対応。

### skill_categories（スキル区分マスタ、RDB厳密カラム）

| カラム | 型 | 説明 |
|---|---|---|
| skill_category_id | uuid | PK（アプリ側で採番） |
| code | text (unique) | 区分コード（例: `language`, `framework`, `cloud`） |
| name | text | 区分名 |
| display_order | integer | 区分の表示順（1始まりの連番） |
| created_by / updated_by | text | 登録者・最終更新者（管理者） |
| created_at / updated_at | timestamp | 作成・更新日時 |

### skill_master_items（星取表マスタ／スキル項目、RDB厳密カラム）

| カラム | 型 | 説明 |
|---|---|---|
| skill_master_item_id | uuid | PK（アプリ側で採番） |
| code | text (unique) | スキルコード（`skill_XX`形式、プレフィックスで項目種別を一意に管理） |
| name | text | スキル項目名 |
| skill_category_id | uuid (FK → skill_categories.skill_category_id) | 区分（言語／フレームワーク／クラウド等） |
| status | text/enum | 状態（有効／アーカイブ） |
| display_order | integer | 区分内での表示順（1始まりの連番） |
| version | integer | 楽観ロック用バージョン |
| created_by / updated_by | text | 登録者・最終更新者（管理者） |
| created_at / updated_at | timestamp | 作成・更新日時 |

### level_categories（レベル区分マスタ、RDB厳密カラム）

| カラム | 型 | 説明 |
|---|---|---|
| level_category_id | uuid | PK（アプリ側で採番） |
| code | text (unique) | 区分コード（例: `infra`, `application`） |
| name | text | 区分名 |
| display_order | integer | 区分の表示順（1始まりの連番） |
| created_by / updated_by | text | 登録者・最終更新者（管理者） |
| created_at / updated_at | timestamp | 作成・更新日時 |

### level_master_items（星取表マスタ／レベル項目、RDB厳密カラム）

| カラム | 型 | 説明 |
|---|---|---|
| level_master_item_id | uuid | PK（アプリ側で採番） |
| code | text (unique) | レベルコード（`level_xx`形式） |
| name | text | レベル項目名（例: 初級／中級／上級など） |
| description | text | レベルの説明 |
| level_category_id | uuid (FK → level_categories.level_category_id) | 区分（インフラ／アプリケーション等、レベル定義が領域によって異なることに対応） |
| status | text/enum | 状態（有効／アーカイブ） |
| display_order | integer | 区分内での表示順（1始まりの連番） |
| version | integer | 楽観ロック用バージョン |
| created_by / updated_by | text | 登録者・最終更新者（管理者） |
| created_at / updated_at | timestamp | 作成・更新日時 |

### user_skills（星取表／スキル・レベル、RDB厳密カラム）

| カラム | 型 | 説明 |
|---|---|---|
| user_skill_id | uuid | PK（アプリ側で採番） |
| account_id | text (FK → accounts.account_id) | 所有者 |
| skill_master_item_id | uuid (FK → skill_master_items.skill_master_item_id) | 登録したスキル項目 |
| level_master_item_id | uuid (FK → level_master_items.level_master_item_id, nullable) | 設定したレベル項目。スキル登録時点ではレベル未設定（NULL）も許容する |
| version | integer | 楽観ロック用バージョン |
| created_by / updated_by | text | 登録者・最終更新者（本人） |
| created_at / updated_at | timestamp | 登録・更新日時 |

`(account_id, skill_master_item_id)`の組で一意（ユーザーごとに同一スキル項目は1行）。

### 補足

- 「区分」は`skill_categories` / `level_categories`という専用マスタテーブルとして切り出し、`skill_master_items` / `level_master_items`から`skill_category_id` / `level_category_id`で参照する（[ui-flows.md 3章](ui-flows.md#3-星取表コンテキスト)の補足で確認した方針）。区分自体の並び替えは各categoriesテーブルの`display_order`、区分内の項目の並び替えは各master_itemsテーブルの`display_order`で行い、いずれも1始まりの連番で運用する（0章）。検索ページの「星取表（スキル×レベル）」条件や、エンジニアページ・マイページでのグルーピング表示・レイアウト切替（[ui-flows.md 3章](ui-flows.md#3-星取表コンテキスト)の補足のUC-S5）に使用する想定。
- `level_categories`（インフラ／アプリケーション等）により、レベル定義が領域ごとに異なる場合に対応する。
- スキルコード／レベルコードは `skill_XX` / `level_xx` のように接頭辞でカテゴリを一意に管理する（CSVメモ「VOでスキルコード・レベルコードをチェックする」方針）。一意性のルール自体はDB制約では強制せず、コード発行・検証はアプリケーション側（値オブジェクト）で行う。
- イベントストーミングでは「スキルの保有登録」と「スキルへのレベル設定」を別イベントとして識別していたが、1ユーザー・1スキル項目に対してレベルは最大1つ（1:1）であるため、`user_skills`に`level_master_item_id`（nullable）を持たせて1テーブルに統合する。「スキルだけ登録（レベル未設定）」は`level_master_item_id = NULL`の行、「レベル設定・変更」は同じ行の`level_master_item_id`の更新として表現する。
- 一括登録系（星取表マスタ一括登録、星取表一括登録（星取表CSV））は[README.md 7章](README.md#7-イベントストーミング結果と業務ルール)の方針通りAPI化せず、上記テーブルへのSQL直接投入で対応する。
- マスタのアーカイブ／アーカイブ解除は`skill_master_items.status` / `level_master_items.status`の状態遷移で表現する。
- `user_skills`への`version`の付与は、[quality-standards.md 1章・6章](quality-standards.md#1-機能適合性functional-suitability)で確定した「編集系テーブルの楽観ロック」方針（0章）に基づく。`created_by` / `updated_by`等の監査カラムは0章の方針により全テーブル共通で付与する。
- `skill_categories` / `skill_master_items` / `level_categories` / `level_master_items`は星取表専用ではなく、経歴書（4章）の`resume_project_skills`からも参照される共通マスタである。テーブル名・配置章は変更せず、本章を「スキル・レベルマスタの定義元」として扱う（4章側で参照関係を補足する）。

## 4. 経歴書コンテキスト

[ui-flows.md 4章](ui-flows.md#4-経歴書コンテキスト)（UC-R1〜R4）に対応。

> 経歴書は「案件名×言語」での検索（[quality-standards.md 1章](quality-standards.md#1-機能適合性functional-suitability)）に対応するため、`resumes.content`へのJSONB集約ではなく、正規化テーブル（`resumes` / `resume_qualifications` / `resume_projects`）で構成する。

### resumes（経歴書本体・基本情報、RDB厳密カラム）

| カラム | 型 | 説明 |
|---|---|---|
| resume_id | uuid | PK（アプリ側で採番） |
| account_id | text (FK → accounts.account_id, unique) | 所有者（本人）。1アカウントにつき経歴書1件 |
| age | integer | 年齢 |
| nearest_station | text | 最寄り駅。個人情報マスク対象列（`view_personal_info` 権限なしの場合はNULL化して返す、[APP-ADR-0007](../adr/APP-ADR-0007-rolesをpermissionベースに再定義しvisibility_rulesを廃止.md)） |
| final_education | text | 最終学歴。個人情報マスク対象列（`view_personal_info` 権限なしの場合はNULL化して返す、[APP-ADR-0007](../adr/APP-ADR-0007-rolesをpermissionベースに再定義しvisibility_rulesを廃止.md)） |
| self_pr | text | 自己PR |
| status | text/enum | 状態（有効／アーカイブ） |
| version | integer | 楽観ロック用バージョン |
| created_by / updated_by | text | 登録者・最終更新者（本人） |
| created_at / updated_at | timestamp | 登録・更新日時 |

### resume_qualifications（経歴書／資格情報、RDB厳密カラム）

| カラム | 型 | 説明 |
|---|---|---|
| resume_qualification_id | uuid | PK（アプリ側で採番） |
| resume_id | uuid (FK → resumes.resume_id) | 所属する経歴書 |
| name | text | 資格名 |
| acquired_date | date (nullable) | 取得年月 |
| display_order | integer | 表示順（1始まりの連番） |
| created_by / updated_by | text | 登録者・最終更新者 |
| created_at / updated_at | timestamp | 作成・更新日時 |

### resume_projects（経歴書／案件経歴、RDB厳密カラム）

| カラム | 型 | 説明 |
|---|---|---|
| resume_project_id | uuid | PK（アプリ側で採番） |
| resume_id | uuid (FK → resumes.resume_id) | 所属する経歴書 |
| period_start | date | 案件期間（開始） |
| period_end | date (nullable) | 案件期間（終了、進行中はNULL） |
| project_name | text | 案件名 |
| description | text | 業務内容 |
| os_db | text[] | OS／DB（複数指定可） |
| position | text | ポジション |
| team_size | integer | 人数 |
| process_phases | text[] | 工程（要件定義／設計／実装／テスト等、複数指定可） |
| display_order | integer | 表示順（1始まりの連番） |
| created_by / updated_by | text | 登録者・最終更新者 |
| created_at / updated_at | timestamp | 作成・更新日時 |

### resume_project_skills（経歴書／案件経歴×使用スキル、RDB厳密カラム）

| カラム | 型 | 説明 |
|---|---|---|
| resume_project_skill_id | uuid | PK（アプリ側で採番） |
| resume_project_id | uuid (FK → resume_projects.resume_project_id) | 所属する案件経歴 |
| skill_master_item_id | uuid (FK → skill_master_items.skill_master_item_id) | 使用した言語・ツール・FW（3章のスキルマスタを参照） |
| created_by / updated_by | text | 登録者・最終更新者 |
| created_at / updated_at | timestamp | 作成・更新日時 |

`(resume_project_id, skill_master_item_id)`の組で一意。

### 補足

- `resumes`を基本情報（旧テーブルA）と集約（旧テーブルD）を統合したテーブル、`resume_qualifications`を資格情報（旧テーブルB）、`resume_projects`を案件経歴（旧テーブルC）として正規化する。`resume_qualifications` / `resume_projects`は`resumes`の子テーブルであり、経歴書全体の編集（UC-R1）はトランザクション内で`resumes.version`を用いて楽観ロックする想定（子テーブル自体には`version`を持たせない。`created_by` / `updated_by`等の監査カラムは0章の方針により全テーブル共通で付与する）。
- 経歴書検索（「案件名×言語」、単体検索可）は`resumes` / `resume_projects` / `resume_project_skills` / `skill_master_items`をJOINし、`resume_projects.project_name`（LIKE等）と`resume_project_skills.skill_master_item_id`（`skill_master_items`経由でのスキル指定）を条件に検索する。
- 経歴書のアーカイブ／アーカイブ解除（UC-R3）は`resumes.status`の状態遷移で表現する。
- 一括登録（経歴書CSV）は[README.md 7章](README.md#7-イベントストーミング結果と業務ルール)の方針通りSQL直接投入で対応する。
- **フォーマット変更への対応**: キャリアシートと異なり、経歴書のフォーマット変更（項目の追加等）はJSONBスキーマ管理ではなく、テーブルへのカラム追加マイグレーションで対応する（[README.md 7章](README.md#7-イベントストーミング結果と業務ルール)で経歴書とキャリアシートのバージョニング要件を分離）。
- **マスク済み経歴書（UC-R2）**: マスク対象は`resumes.nearest_station`（最寄り駅）と`resumes.final_education`（最終学歴）の2列。`age`（年齢）・`self_pr`・`resume_qualifications`・`resume_projects`はマスク対象外で本人以外にも公開する。マスクの適用・解除は以下の通り判定する（[APP-ADR-0007](../adr/APP-ADR-0007-rolesをpermissionベースに再定義しvisibility_rulesを廃止.md)）。
  - 閲覧者が経歴書の所有者本人（`resumes.account_id` = 閲覧者の`account_id`）の場合: マスクしない（UC-R1のマイページ表示）。
  - 閲覧者が本人以外の場合: `account_roles`経由で閲覧者が`view_personal_info`権限を持っていればマスクしない。権限がなければ`nearest_station` / `final_education`をNULL化（または非選択）して返す。
- **経歴書更新→星取表反映（UC-R1→星取表連携）**: 経歴書の言語・ツール入力（`resume_project_skills`）は自由入力ではなく3章の`skill_master_items`からの選択とするため、経歴書保存時に`resume_project_skills`へ登録された各`skill_master_item_id`について、保存者本人の`user_skills`（3章）に未登録であれば`level_master_item_id = NULL`の行をUPSERTする。既存の`user_skills`行（レベル設定済み含む）はそのまま保持し、上書きしない。
  - 経歴書保存（UC-R1）自体は新規スキル検出有無に関わらず即時完了させる（保存処理をブロックしない）。
  - 保存後、新規追加された`user_skills`行（`level_master_item_id = NULL`）がある場合は、レベル入力を促す確認ダイアログを表示する（スキップ可）。入力された場合はその場で`level_master_item_id`を更新し、スキップした場合はNULLのまま残す。
  - `level_master_item_id = NULL`の行は「レベル未設定」として星取表の陳腐化防止のため、マイページ等で件数バッジ等のリマインダー表示を行う（[ui-flows.md 0章](ui-flows.md#0-全体画面構成)）。レベルの設定・変更は星取表編集（UC-S3）で行う。
- 経歴書項目検索・一覧（UC-R4、管理者画面）はQuery側で`resumes` / `resume_projects`をJOINしてMyBatisで直接マッピングして返す想定。

## 5. ファイルコンテキスト

[ui-flows.md 5章](ui-flows.md#5-ファイルコンテキスト)（UC-F1）に対応。

### 補足

- UC-F1（経歴書ファイル出力）は、`resumes` / `resume_qualifications` / `resume_projects`（4章）を基にExcel/PDFを動的に生成する処理であり、出力ファイル自体を永続化するデータモデルは現時点では想定しない（[README.md 4章](README.md#4-アーキテクチャ原則adr化予定の方針)のExcel正・PDF変換方針）。経歴書はJSONBスキーマ管理ではないため、フォーマットバージョン（キャリアシート4章想定）の概念は経歴書出力には適用しない。
- **出力対象**: 出力対象は経歴書（`resumes` / `resume_qualifications` / `resume_projects`）のみとし、星取表（`user_skills`、3章）は出力対象外とする。
- **出力履歴**: 出力履歴（誰がいつ誰の経歴書を出力したか）を記録するテーブル（`resume_export_logs`等）はStep1では設けない。
