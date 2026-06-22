package com.kakehashi.usecase.account

import com.kakehashi.domain.account.AccountId
import com.kakehashi.domain.account.AccountRepository
import com.kakehashi.domain.account.AccountStatus
import com.kakehashi.usecase.account.exception.AccountNotFoundException
import com.kakehashi.usecase.account.exception.ForbiddenOperationException
import com.kakehashi.usecase.account.exception.InvalidStatusTransitionException
import com.kakehashi.usecase.account.exception.OptimisticLockException
import java.time.OffsetDateTime

/**
 * UC-A7: アカウント停止解除（管理者）UseCase
 *
 * 根拠: docs/design/api/account-role.md（UC-A7 アカウント停止解除）
 * - admin 権限保持者のみ実行可能（UseCase 内で isAdmin フラグを検証）
 * - accounts.status を active、suspended_at を NULL に設定する
 * - APP-ADR-0005: version による楽観ロック
 * - suspended 状態でない場合は 409 Conflict（InvalidStatusTransitionException）
 */
class UnsuspendAccountUseCase(
    private val accountRepository: AccountRepository,
) {
    data class Input(
        val targetAccountId: AccountId,
        val operatorAccountId: String,
        val isAdmin: Boolean,
        val version: Int,
    )

    data class Output(
        val accountId: String,
        val status: AccountStatus,
        val suspendedAt: OffsetDateTime?,
        val version: Int,
    )

    /**
     * 対象アカウントの停止を解除し、SUSPENDED → ACTIVE へ遷移させる（管理者のみ実行可能）。
     *
     * 設計書No：UC-A7
     * ADRNo：APP-ADR-0005, APP-ADR-0008
     *
     * @param input 対象アカウントID・操作者ID・isAdmin・version を含む入力値
     * @throws ForbiddenOperationException isAdmin=false の場合
     * @throws AccountNotFoundException 対象アカウントが存在しない場合
     * @throws InvalidStatusTransitionException SUSPENDED 以外のステータスからの解除試行
     * @throws OptimisticLockException version 不一致または DB 更新 0件の場合
     */
    fun execute(input: Input): Output {
        // admin 権限チェック（UC-A7: 管理者のみ実行可能）
        if (!input.isAdmin) {
            throw ForbiddenOperationException("アカウントの停止解除は管理者権限が必要です")
        }

        val account =
            accountRepository.findById(input.targetAccountId)
                ?: throw AccountNotFoundException(input.targetAccountId.value)

        // version 不一致チェック（楽観ロック）
        if (account.version != input.version) {
            throw OptimisticLockException(input.targetAccountId.value, input.version, account.version)
        }

        // UC-A7: SUSPENDED のみ実行可能（それ以外は 409）
        if (account.status != AccountStatus.SUSPENDED) {
            throw InvalidStatusTransitionException(
                accountId = input.targetAccountId.value,
                from = account.status,
                to = AccountStatus.ACTIVE,
            )
        }

        val unsuspended = account.unsuspend(updatedBy = input.operatorAccountId)
        val rows = accountRepository.update(unsuspended)
        if (rows == 0) {
            val currentVersion = accountRepository.findById(input.targetAccountId)?.version ?: -1
            throw OptimisticLockException(input.targetAccountId.value, input.version, currentVersion)
        }

        return Output(
            accountId = unsuspended.accountId.value,
            status = unsuspended.status,
            suspendedAt = null,
            version = unsuspended.version,
        )
    }
}
