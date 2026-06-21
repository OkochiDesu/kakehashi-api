-- V2__add_account_id_seq.sql
-- accounts.account_id 採番用シーケンス（AZ0001〜AZ9999）
-- 根拠: docs/design/api/account-role.md（accounts.account_id の形式）
--       docs/requirements/data-models.md 1章

CREATE SEQUENCE accounts_account_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 9999
    NO CYCLE;

COMMENT ON SEQUENCE accounts_account_id_seq IS 'accounts.account_id 採番用シーケンス（AZ%04d 形式, 1〜9999）';
