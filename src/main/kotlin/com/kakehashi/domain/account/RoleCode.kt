package com.kakehashi.domain.account

/**
 * 権限コード Enum
 *
 * 根拠: docs/requirements/data-models.md 1章・APP-ADR-0007
 * roles テーブルは「できること（Permission）」のマスタ。Step1の初期データは admin / view_personal_info の2種類。
 */
enum class RoleCode(
    val code: String,
) {
    ADMIN("admin"),
    VIEW_PERSONAL_INFO("view_personal_info"),
    ;

    companion object {
        /**
         * code 文字列から RoleCode に変換する。
         *
         * 設計書No：-
         * ADRNo：APP-ADR-0007
         *
         * @param code roles.code カラム値（例: "admin"）
         * @throws IllegalArgumentException 未知の code の場合
         */
        fun fromCode(code: String): RoleCode =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("不明なロールコードです: $code")
    }
}
