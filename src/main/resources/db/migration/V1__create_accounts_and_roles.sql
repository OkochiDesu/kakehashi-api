-- V1__create_accounts_and_roles.sql
-- ドメイン1: アカウント・ロール
-- 根拠: docs/requirements/data-models.md 1章
--       docs/adr/ADR-0006（テーブル設計共通方針）
--       docs/adr/ADR-0008（ロール別可視範囲・visibility_rules初期データ）
--       docs/adr/ADR-0009（Flyway運用方針）

-- =============================================================
-- accounts
-- =============================================================
-- account_id: AZ0000形式のtext型（社員コード相当、アプリ側採番）
-- ADR-0006: PKカラム名は<エンティティ名>_id に統一
-- ADR-0006: 監査カラム（created_by/updated_by/created_at/updated_at）は全テーブル共通
-- ADR-0006: 編集系テーブルには楽観ロック用 version を付与
-- created_by/updated_by にFKを持たせない理由: ユーザー操作とバッチ処理の識別子が混在するため（ADR-0006決定1）
CREATE TABLE accounts (
    account_id    TEXT        NOT NULL,
    google_sub_hash TEXT      NOT NULL,
    email         TEXT        NOT NULL,
    name          TEXT        NOT NULL,
    -- 'provisional'（仮登録）/ 'active'（本登録）/ 'suspended'（停止）
    status        TEXT        NOT NULL,
    suspended_at  TIMESTAMPTZ,
    version       INTEGER     NOT NULL DEFAULT 0,
    created_by    TEXT        NOT NULL,
    updated_by    TEXT        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_accounts PRIMARY KEY (account_id),
    CONSTRAINT uq_accounts_google_sub_hash UNIQUE (google_sub_hash)
);

-- google_sub_hash はログイン時の照合で使用（data-models.md 1章）
CREATE INDEX idx_accounts_status ON accounts (status);

-- =============================================================
-- roles
-- =============================================================
-- role_id: UUID型（アプリ側でUUID v7を採番）
-- PostgreSQLのgen_random_uuid()はデフォルト値に使わない（ADR-0006決定3）
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

-- =============================================================
-- account_roles
-- =============================================================
-- アカウントと複数ロールの多対多（ADR-0006 / data-models.md 1章の補足）
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

-- =============================================================
-- visibility_rules
-- =============================================================
-- ロール別に対象カテゴリの閲覧可否を制御するテーブル（ADR-0008決定1・4）
-- target_category: 可視性を制御する対象区分のコード文字列
--   Step1では 'resume_personal_info'（最寄り駅・最終学歴）のみを想定
-- ADR-0008決定4: 将来target_categoryが増える場合はINSERTで対応し、テーブル構造変更は不要
CREATE TABLE visibility_rules (
    visibility_rule_id UUID        NOT NULL,
    role_id            UUID        NOT NULL,
    target_category    TEXT        NOT NULL,
    can_view           BOOLEAN     NOT NULL,
    created_by         TEXT        NOT NULL,
    updated_by         TEXT        NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_visibility_rules PRIMARY KEY (visibility_rule_id),
    CONSTRAINT fk_visibility_rules_role FOREIGN KEY (role_id) REFERENCES roles (role_id),
    -- ロールとカテゴリの組み合わせは一意
    CONSTRAINT uq_visibility_rules_role_category UNIQUE (role_id, target_category)
);

CREATE INDEX idx_visibility_rules_role_id ON visibility_rules (role_id);

-- =============================================================
-- 初期データ: roles
-- =============================================================
-- ADR-0008決定4: Step1で定義するロールは general / sales / admin の3種類
-- code・name・display_orderは固定値として確定
-- created_by/updated_by: 初期投入バッチを識別するリクエストID相当の文字列（ADR-0006決定1）
INSERT INTO roles (role_id, code, name, created_by, updated_by)
VALUES
    ('01970000-0000-7000-8000-000000000001', 'general', '一般（エンジニア）', 'system:init', 'system:init'),
    ('01970000-0000-7000-8000-000000000002', 'sales',   '営業',             'system:init', 'system:init'),
    ('01970000-0000-7000-8000-000000000003', 'admin',   '管理者',           'system:init', 'system:init');

-- =============================================================
-- 初期データ: visibility_rules
-- =============================================================
-- ADR-0008決定4のロール別可視範囲に従う:
--   general: resume_personal_info → can_view = false（一般エンジニアはマスクして閲覧）
--   sales:   resume_personal_info → can_view = true （営業は全項目閲覧可）
--   admin:   resume_personal_info → can_view = true （管理者は全項目閲覧可）
INSERT INTO visibility_rules (visibility_rule_id, role_id, target_category, can_view, created_by, updated_by)
VALUES
    ('01970000-0000-7000-8000-000000000011',
     '01970000-0000-7000-8000-000000000001',
     'resume_personal_info', false, 'system:init', 'system:init'),
    ('01970000-0000-7000-8000-000000000012',
     '01970000-0000-7000-8000-000000000002',
     'resume_personal_info', true,  'system:init', 'system:init'),
    ('01970000-0000-7000-8000-000000000013',
     '01970000-0000-7000-8000-000000000003',
     'resume_personal_info', true,  'system:init', 'system:init');
