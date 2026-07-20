package com.kakehashi.usecase.account

import com.kakehashi.domain.account.AccountId
import com.kakehashi.domain.account.AccountRepository
import com.kakehashi.domain.account.RoleCode
import com.kakehashi.usecase.account.exception.AccountNotFoundException
import com.kakehashi.usecase.account.exception.ForbiddenOperationException
import com.kakehashi.usecase.account.exception.OptimisticLockException
import java.util.UUID

/**
 * UC-A6: 権限付与・変更（管理者）UseCase
 *
 * 根拠: docs/design/api/account-role.md（UC-A6）
 * - admin 権限保持者のみ実行可能（呼び出し元の Controller / 将来の @PreAuthorize で保証）
 * - account_roles を全置換する（リクエストで true を指定した権限を挿入、false を指定した権限を削除）
 * - APP-ADR-0005: accounts.version による楽観ロック
 * - APP-ADR-0007: roles は Permission マスタ（admin / view_personal_info の 2 種）
 */
class AssignRolesUseCase(
    private val accountRepository: AccountRepository,
) {
    // roles テーブルの初期データ UUID（V1 migration で固定値を投入）
    companion object {
        private val ADMIN_ROLE_ID: UUID = UUID.fromString("01970000-0000-7000-8000-000000000001")
        private val VIEW_PERSONAL_INFO_ROLE_ID: UUID = UUID.fromString("01970000-0000-7000-8000-000000000002")

        fun roleIdFor(roleCode: RoleCode): UUID =
            when (roleCode) {
                RoleCode.ADMIN -> ADMIN_ROLE_ID
                RoleCode.VIEW_PERSONAL_INFO -> VIEW_PERSONAL_INFO_ROLE_ID
            }
    }

    data class Input(
        val targetAccountId: AccountId,
        val operatorAccountId: String,
        val operatorIsAdmin: Boolean,
        val grantAdminRole: Boolean,
        val grantViewPersonalInfoRole: Boolean,
        val version: Int,
    )

    data class RoleOutput(
        val roleId: String,
        val code: String,
        val name: String,
    )

    data class Output(
        val accountId: String,
        val roles: List<RoleOutput>,
        val version: Int,
    )

    /**
     * 対象アカウントのロールを全置換する（管理者のみ実行可能）。
     *
     * 設計書No：UC-A6
     * ADRNo：APP-ADR-0005, APP-ADR-0007, APP-ADR-0008
     *
     * @param input 対象アカウントID・操作者ID・ロールフラグ・version を含む入力値
     * @throws ForbiddenOperationException operatorIsAdmin=false の場合
     * @throws AccountNotFoundException 対象アカウントが存在しない場合
     * @throws OptimisticLockException version 不一致または DB 更新 0件の場合
     */
    fun execute(input: Input): Output {
        // admin 権限チェック（UC-A6: 管理者のみ実行可能）
        if (!input.operatorIsAdmin) {
            throw ForbiddenOperationException("ロールの付与・変更は管理者権限が必要です")
        }

        val account =
            accountRepository.findById(input.targetAccountId)
                ?: throw AccountNotFoundException(input.targetAccountId.value)

        // version 不一致チェック（楽観ロック）
        if (account.version != input.version) {
            throw OptimisticLockException(input.targetAccountId.value, input.version, account.version)
        }

        // 付与するロール ID リストを構築
        val newRoleIds =
            buildList<UUID> {
                if (input.grantAdminRole) add(ADMIN_ROLE_ID)
                if (input.grantViewPersonalInfoRole) add(VIEW_PERSONAL_INFO_ROLE_ID)
            }

        // version をインクリメントした Account（トランザクション内で update する）
        val versionBumped = account.assignRoles(input.operatorAccountId)

        // account_roles 全置換 + accounts.version インクリメントを1トランザクションで実行（修正4）
        val rows =
            accountRepository.assignRolesAndBumpVersion(
                accountId = input.targetAccountId,
                roleIds = newRoleIds,
                account = versionBumped,
                operatorId = input.operatorAccountId,
            )
        if (rows == 0) {
            val currentVersion = accountRepository.findById(input.targetAccountId)?.version ?: -1
            throw OptimisticLockException(input.targetAccountId.value, input.version, currentVersion)
        }

        // レスポンス用 roles リストを組み立て
        val roles =
            buildList {
                if (input.grantAdminRole) {
                    add(
                        RoleOutput(
                            roleId = ADMIN_ROLE_ID.toString(),
                            code = RoleCode.ADMIN.code,
                            name = "管理業務",
                        ),
                    )
                }
                if (input.grantViewPersonalInfoRole) {
                    add(
                        RoleOutput(
                            roleId = VIEW_PERSONAL_INFO_ROLE_ID.toString(),
                            code = RoleCode.VIEW_PERSONAL_INFO.code,
                            name = "個人情報表示",
                        ),
                    )
                }
            }

        return Output(
            accountId = input.targetAccountId.value,
            roles = roles,
            version = versionBumped.version,
        )
    }
}
