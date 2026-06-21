-- V1__create_accounts_and_roles.sql
-- ドメイン1: アカウント・ロール
-- 根拠: docs/requirements/data-models.md 1章
--       docs/adr/APP-ADR-0001（テーブル設計共通方針）
--       docs/adr/APP-ADR-0003（ロール別可視範囲・visibility_rules初期データ）
--       docs/adr/APP-ADR-0004（Flyway運用方針）

-- =============================================================
-- accounts
-- =============================================================
-- account_id: AZ0000形式のtext型（社員コード相当、アプリ側採番）
-- APP-ADR-0001: PKカラム名は<エンティティ名>_id に統一
-- APP-ADR-0001: 監査カラム（created_by/updated_by/created_at/updated_at）は全テーブル共通
-- APP-ADR-0001: 編集系テーブルには楽観ロック用 version を付与
-- created_by/updated_by にFKを持たせない理由: ユーザー操作とバッチ処理の識別子が混在するため（APP-ADR-0001決定1）
CREATE TABLE accounts (
    account_id    TEXT        NOT NULL,
    google_sub_hash TEXT      NOT NULL,
    email         TEXT        NOT NULL,
    name          TEXT        NOT NULL,
    -- 'provisional'（仮登録）/ 'active'（本登録）/ 'suspended'（停止）/ 'deactivated'（廃止・1年停止で自動遷移）
    status        TEXT        NOT NULL,
    suspended_at  TIMESTAMPTZ,
    version       INTEGER     NOT NULL DEFAULT 0,
    created_by    TEXT        NOT NULL,
    updated_by    TEXT        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_accounts PRIMARY KEY (account_id),
    CONSTRAINT uq_accounts_google_sub_hash UNIQUE (google_sub_hash),
    -- APP-ADR-0006: status の有効値を制約（deactivated は suspended から1年経過後に @Scheduled で自動遷移）
    CONSTRAINT ck_accounts_status CHECK (status IN ('provisional', 'active', 'suspended', 'deactivated'))
);

COMMENT ON TABLE  accounts                        IS 'アカウント';
COMMENT ON COLUMN accounts.account_id             IS 'アカウントID（社員コード相当、AZ0000形式）';
COMMENT ON COLUMN accounts.google_sub_hash        IS 'Google OAuth subクレームのハッシュ値';
COMMENT ON COLUMN accounts.email                  IS 'メールアドレス';
COMMENT ON COLUMN accounts.name                   IS '氏名';
COMMENT ON COLUMN accounts.status                 IS 'ステータス（provisional: 仮登録 / active: 本登録 / suspended: 停止 / deactivated: 廃止）（APP-ADR-0006）';
COMMENT ON COLUMN accounts.suspended_at           IS '停止日時。suspended_atから1年経過でdeactivatedへ自動遷移（@Scheduled日次）（APP-ADR-0006）';
COMMENT ON COLUMN accounts.version                IS '楽観ロック用バージョン番号';
COMMENT ON COLUMN accounts.created_by             IS '作成者（アカウントIDまたはバッチのリクエストID）';
COMMENT ON COLUMN accounts.updated_by             IS '更新者（アカウントIDまたはバッチのリクエストID）';
COMMENT ON COLUMN accounts.created_at             IS '作成日時';
COMMENT ON COLUMN accounts.updated_at             IS '更新日時';

-- google_sub_hash はログイン時の照合で使用（data-models.md 1章）
CREATE INDEX idx_accounts_status ON accounts (status);

-- =============================================================
-- roles
-- =============================================================
-- role_id: UUID型（アプリ側でUUID v7を採番）
-- PostgreSQLのgen_random_uuid()はデフォルト値に使わない（APP-ADR-0001決定3）
-- display_order: roles自体は固定マスタのため定義しないが、
--   data-models.mdにdisplay_orderの記載がないため付与しない（推測カラム禁止）
CREATE TABLE roles (
    role_id    UUID        NOT NULL,
    code       TEXT        NOT NULL,
    name       TEXT        NOT NULL,
    created_by TEXT        NOT NULL,
    updated_by TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_roles PRIMARY KEY (role_id),
    CONSTRAINT uq_roles_code UNIQUE (code)
);

COMMENT ON TABLE  roles            IS 'ロール（区分）マスタ';
COMMENT ON COLUMN roles.role_id    IS 'ロールID（UUID v7、アプリ側採番）';
COMMENT ON COLUMN roles.code       IS 'ロールコード（例: admin, view_personal_info）（APP-ADR-0007）';
COMMENT ON COLUMN roles.name       IS 'ロール名称（例: 一般（エンジニア）/ 営業 / 管理者）';
COMMENT ON COLUMN roles.created_by IS '作成者';
COMMENT ON COLUMN roles.updated_by IS '更新者';
COMMENT ON COLUMN roles.created_at IS '作成日時';
COMMENT ON COLUMN roles.updated_at IS '更新日時';

-- =============================================================
-- account_roles
-- =============================================================
-- アカウントと複数ロールの多対多（APP-ADR-0001 / data-models.md 1章の補足）
CREATE TABLE account_roles (
    account_role_id UUID        NOT NULL,
    account_id      TEXT        NOT NULL,
    role_id         UUID        NOT NULL,
    created_by      TEXT        NOT NULL,
    updated_by      TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_account_roles PRIMARY KEY (account_role_id),
    CONSTRAINT fk_account_roles_account FOREIGN KEY (account_id) REFERENCES accounts (account_id),
    CONSTRAINT fk_account_roles_role    FOREIGN KEY (role_id)    REFERENCES roles (role_id),
    -- 1アカウントに同一ロールを重複付与しない
    CONSTRAINT uq_account_roles_account_role UNIQUE (account_id, role_id)
);

CREATE INDEX idx_account_roles_account_id ON account_roles (account_id);
CREATE INDEX idx_account_roles_role_id    ON account_roles (role_id);

COMMENT ON TABLE  account_roles                    IS 'アカウント・ロール紐付け（多対多）';
COMMENT ON COLUMN account_roles.account_role_id    IS 'アカウントロールID（UUID v7、アプリ側採番）';
COMMENT ON COLUMN account_roles.account_id         IS 'アカウントID';
COMMENT ON COLUMN account_roles.role_id            IS 'ロールID';
COMMENT ON COLUMN account_roles.created_by         IS '作成者';
COMMENT ON COLUMN account_roles.updated_by         IS '更新者';
COMMENT ON COLUMN account_roles.created_at         IS '作成日時';
COMMENT ON COLUMN account_roles.updated_at         IS '更新日時';

-- =============================================================
-- 初期データ: roles
-- =============================================================
-- APP-ADR-0007: rolesは権限（Permission）マスタ。職種ベースではなく「できること」を表す
-- Step1で定義する権限は admin / view_personal_info の2種類
-- created_by/updated_by: 初期投入バッチを識別するリクエストID相当の文字列（APP-ADR-0001決定1）
INSERT INTO roles (role_id, code, name, created_by, updated_by)
VALUES
    ('01970000-0000-7000-8000-000000000001', 'admin',             '管理業務',     'system:init', 'system:init'),
    ('01970000-0000-7000-8000-000000000002', 'view_personal_info','個人情報表示', 'system:init', 'system:init');
