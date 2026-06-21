package com.kakehashi.domain.account

/**
 * アカウントID 値オブジェクト
 *
 * 根拠: docs/requirements/data-models.md 1章（AZ0000 形式）
 * APP-ADR-0001 決定3: PKカラム名は <エンティティ名>_id に統一
 * フォーマット: "AZ%04d".format(seq) → AZ0001〜AZ9999
 */
@JvmInline
value class AccountId(
    val value: String,
) {
    init {
        require(value.matches(PATTERN)) { "Invalid account_id format: $value (expected AZ[0-9]{4})" }
    }

    companion object {
        private val PATTERN = Regex("AZ\\d{4}")

        /**
         * PostgreSQL シーケンス値から AccountId を生成する
         * @param seq nextval('accounts_account_id_seq') の戻り値
         */
        fun fromSequence(seq: Long): AccountId = AccountId("AZ%04d".format(seq))
    }
}
