package com.kakehashi.usecase.account

import com.kakehashi.domain.account.AccountStatus
import com.kakehashi.domain.account.RoleCode
import com.kakehashi.infrastructure.account.AccountMapper

/**
 * UC-A5: アカウント一覧・検索（管理者）Query UseCase
 *
 * 根拠: docs/design/api/account-role.md（UC-A5）
 * APP-ADR-0008: Query 側は MyBatis Mapper → DTO 直接マッピング（ドメイン層バイパス）
 *
 * 検索可視性ルール（docs/design/api/account-role.md アカウントステータス設計と可視性制御）:
 * - status 未指定: active のみ
 * - admin 権限あり + status 明示: 指定した status のみ返す
 * - admin 権限なし + status 明示: active に強制
 * - deactivated アカウントの name は "***" でマスク
 */
class ListAccountsQuery(
    private val accountMapper: AccountMapper,
) {
    data class Input(
        val name: String?,
        val statuses: List<String>?,
        val roleCode: String?,
        val page: Int = 0,
        val size: Int = 20,
        val isAdmin: Boolean,
    )

    data class AccountSummary(
        val accountId: String,
        val name: String,
        val status: String,
    )

    data class Output(
        val content: List<AccountSummary>,
        val totalElements: Long,
        val totalPages: Int,
        val page: Int,
        val size: Int,
    )

    /**
     * アカウント一覧を検索して返す。非管理者は status が active に強制される。
     *
     * 設計書No：UC-A5
     * ADRNo：APP-ADR-0006, APP-ADR-0008
     *
     * @param input 検索条件（name / statuses / roleCode / ページング / isAdmin）
     * @return ページング済みアカウント一覧（deactivated の name は "***" にマスク）
     */
    fun execute(input: Input): Output {
        // admin 権限なしは status を active 固定に強制
        val effectiveStatuses =
            if (input.isAdmin && !input.statuses.isNullOrEmpty()) {
                input.statuses
            } else {
                listOf(AccountStatus.ACTIVE.toDbValue())
            }

        val roleCode =
            input.roleCode?.let {
                runCatching { RoleCode.fromCode(it) }.getOrNull()?.code
            }

        val offset = input.page * input.size
        val rows =
            accountMapper.searchAccounts(
                name = input.name,
                statuses = effectiveStatuses,
                roleCode = roleCode,
                limit = input.size,
                offset = offset,
            )
        val totalElements =
            accountMapper.countAccounts(
                name = input.name,
                statuses = effectiveStatuses,
                roleCode = roleCode,
            )
        val totalPages = if (input.size == 0) 0 else ((totalElements + input.size - 1) / input.size).toInt()

        val content =
            rows.map { row ->
                val maskedName =
                    if (row.status == AccountStatus.DEACTIVATED.toDbValue()) "***" else row.name
                AccountSummary(
                    accountId = row.accountId,
                    name = maskedName,
                    status = row.status,
                )
            }

        return Output(
            content = content,
            totalElements = totalElements,
            totalPages = totalPages,
            page = input.page,
            size = input.size,
        )
    }
}
