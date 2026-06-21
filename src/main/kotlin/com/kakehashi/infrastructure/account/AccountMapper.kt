package com.kakehashi.infrastructure.account

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

/**
 * アカウント MyBatis Mapper（Query 系 DTO マッピング）
 *
 * 根拠: docs/architecture/package-structure.md（infrastructure 層の責務）
 * APP-ADR-0008: Query 側は MyBatis Mapper でドメイン層をバイパスし、JOIN 結果を DTO に直接マッピング
 *
 * SQL は src/main/resources/mapper/account/AccountMapper.xml に記述する
 */
@Mapper
interface AccountMapper {
    /**
     * アカウント一覧・検索（UC-A5）
     * 動的条件（name 部分一致 / status 複数 / roleCode JOIN）は XML の <if> で制御
     */
    fun searchAccounts(
        @Param("name") name: String?,
        @Param("statuses") statuses: List<String>,
        @Param("roleCode") roleCode: String?,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int,
    ): List<AccountSummaryRow>

    /**
     * 検索条件に一致するアカウント総件数
     */
    fun countAccounts(
        @Param("name") name: String?,
        @Param("statuses") statuses: List<String>,
        @Param("roleCode") roleCode: String?,
    ): Long

    /**
     * アカウント詳細取得（account_roles / roles を JOIN）
     */
    fun findAccountDetailById(
        @Param("accountId") accountId: String,
    ): AccountDetailRow?
}

/** アカウント一覧用 DTO */
data class AccountSummaryRow(
    val accountId: String,
    val name: String,
    val status: String,
)

/** アカウント詳細用 DTO */
data class AccountDetailRow(
    val accountId: String,
    val name: String,
    val email: String,
    val status: String,
    val suspendedAt: String?,
    val version: Int,
    val createdAt: String,
    val updatedAt: String,
    val updatedBy: String,
    val roles: List<RoleRow>,
)

/** ロール情報 DTO */
data class RoleRow(
    val roleId: String,
    val code: String,
    val name: String,
)
