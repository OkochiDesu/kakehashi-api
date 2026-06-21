package com.kakehashi.usecase.account

import com.kakehashi.domain.account.AccountId
import com.kakehashi.domain.account.AccountStatus
import com.kakehashi.infrastructure.account.AccountMapper
import com.kakehashi.usecase.account.exception.AccountNotFoundException
import com.kakehashi.usecase.account.exception.ForbiddenOperationException

/**
 * アカウント詳細取得 Query UseCase
 *
 * 根拠: docs/design/api/account-role.md（アカウント詳細取得）
 * APP-ADR-0008: Query 側は MyBatis Mapper → DTO 直接マッピング（ドメイン層バイパス）
 *
 * アクセス制御:
 * - admin 権限あり: 任意の accountId にアクセス可
 * - admin 権限なし: 本人の accountId のみ可（他人は 403）
 *
 * deactivated アカウントは name / email を "***" でマスク
 */
class GetAccountQuery(
    private val accountMapper: AccountMapper,
) {
    data class Input(
        val targetAccountId: AccountId,
        val requestAccountId: String,
        val isAdmin: Boolean,
    )

    data class RoleOutput(
        val roleId: String,
        val code: String,
        val name: String,
    )

    data class Output(
        val accountId: String,
        val name: String,
        val email: String,
        val status: String,
        val roles: List<RoleOutput>,
        val suspendedAt: String?,
        val version: Int,
        val createdAt: String,
        val updatedAt: String,
        val updatedBy: String,
    )

    /**
     * アカウント詳細を取得する。非管理者は自分のアカウントのみ参照可能。
     *
     * 設計書No：-
     * ADRNo：APP-ADR-0006, APP-ADR-0008
     *
     * @param input 取得対象アカウントID・リクエスト者ID・isAdmin
     * @return アカウント詳細（deactivated の name / email は "***" にマスク）
     * @throws ForbiddenOperationException 非管理者が他人のアカウントにアクセスした場合
     * @throws AccountNotFoundException アカウントが存在しない場合
     */
    fun execute(input: Input): Output {
        // 認可チェック: admin 権限なしで他人のアカウントを参照しようとした場合
        if (!input.isAdmin && input.targetAccountId.value != input.requestAccountId) {
            throw ForbiddenOperationException(
                "Account ${input.requestAccountId} is not allowed to access ${input.targetAccountId.value}",
            )
        }

        val row =
            accountMapper.findAccountDetailById(input.targetAccountId.value)
                ?: throw AccountNotFoundException(input.targetAccountId.value)

        // deactivated アカウントは name / email をマスク
        val isDeactivated = row.status == AccountStatus.DEACTIVATED.toDbValue()
        val maskedName = if (isDeactivated) "***" else row.name
        val maskedEmail = if (isDeactivated) "***" else row.email

        return Output(
            accountId = row.accountId,
            name = maskedName,
            email = maskedEmail,
            status = row.status,
            roles =
                row.roles.map { r ->
                    RoleOutput(roleId = r.roleId, code = r.code, name = r.name)
                },
            suspendedAt = row.suspendedAt,
            version = row.version,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
            updatedBy = row.updatedBy,
        )
    }
}
