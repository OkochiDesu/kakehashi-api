package com.kakehashi.domain.account

/**
 * アカウントステータス Enum
 *
 * 根拠: docs/requirements/data-models.md 1章・docs/design/api/account-role.md（アカウントステータス設計）
 * APP-ADR-0006: suspended_at から1年後に deactivated へ自動遷移
 */
enum class AccountStatus {
    PROVISIONAL,
    ACTIVE,
    SUSPENDED,
    DEACTIVATED,
    ;

    /**
     * ログイン可否を返す。
     *
     * 設計書No：-
     * ADRNo：APP-ADR-0006
     *
     * @return ACTIVE の場合のみ true
     */
    fun canLogin(): Boolean = this == ACTIVE

    /**
     * 指定ステータスへの遷移が有効かを返す。
     *
     * 設計書No：UC-A3/A7
     * ADRNo：APP-ADR-0006
     *
     * 遷移規則:
     * - PROVISIONAL → ACTIVE （本登録申込み UC-A3）
     * - ACTIVE → SUSPENDED （停止 UC-A7）
     * - SUSPENDED → ACTIVE （停止解除 UC-A7）
     * - SUSPENDED → DEACTIVATED （@Scheduled 日次バッチ、1年経過後）
     * - DEACTIVATED → なし（廃止後の遷移は不可）
     *
     * @param next 遷移先ステータス
     * @return 遷移可能であれば true
     */
    fun canTransitionTo(next: AccountStatus): Boolean =
        when (this) {
            PROVISIONAL -> next == ACTIVE
            ACTIVE -> next == SUSPENDED
            SUSPENDED -> next == ACTIVE || next == DEACTIVATED
            DEACTIVATED -> false
        }

    /**
     * 一般検索の対象かを返す（ACTIVE のみ true）。
     *
     * 設計書No：UC-A5
     * ADRNo：APP-ADR-0006
     */
    fun isSearchable(): Boolean = this == ACTIVE

    /**
     * DEACTIVATED ステータスかを返す（デフォルト検索では非表示）。
     *
     * 設計書No：-
     * ADRNo：APP-ADR-0006
     */
    fun isDeactivated(): Boolean = this == DEACTIVATED

    /**
     * DB の status カラム値（小文字スネークケース）を返す。
     *
     * 設計書No：-
     * ADRNo：APP-ADR-0006
     */
    fun toDbValue(): String = name.lowercase()

    companion object {
        /**
         * DB の status カラム値から AccountStatus に変換する。
         *
         * 設計書No：-
         * ADRNo：APP-ADR-0006
         *
         * @param value DB のカラム値（例: "active"）
         * @throws IllegalArgumentException 未知の値の場合
         */
        fun fromDbValue(value: String): AccountStatus =
            entries.firstOrNull { it.name.lowercase() == value.lowercase() }
                ?: throw IllegalArgumentException("不明なアカウントステータスです: $value")
    }
}
