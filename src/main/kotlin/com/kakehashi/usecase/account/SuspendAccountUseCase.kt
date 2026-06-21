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
 * UC-A7: アカウント停止（管理者）UseCase
 *
 * 根拠: docs/design/api/account-role.md（UC-A7 アカウント停止）
 * - admin 権限保持者のみ実行可能（呼び出し元で保証）
 * - accounts.status を suspended、suspended_at に現在日時を設定する
 * - APP-ADR-0005: version による楽観ロック
 * - 既に suspended の場合は 409 Conflict（InvalidStatusTransitionException）
 */
class SuspendAccountUseCase(
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
        val suspendedAt: OffsetDateTime,
        val version: Int,
    )

    /**
     * 対象アカウントを停止し、ACTIVE → SUSPENDED へ遷移させる（管理者のみ実行可能）。
     *
     * 設計書No：UC-A7
     * ADRNo：APP-ADR-0005, APP-ADR-0008
     *
     * @param input 対象アカウントID・操作者ID・isAdmin・version を含む入力値
     * @throws ForbiddenOperationException isAdmin=false の場合
     * @throws AccountNotFoundException 対象アカウントが存在しない場合
     * @throws InvalidStatusTransitionException ACTIVE 以外のステータスからの停止試行
     * @throws OptimisticLockException version 不一致または DB 更新 0件の場合
     */
    fun execute(input: Input): Output {
        // admin 権限チェック（UC-A7: 管理者のみ実行可能）
        if (!input.isAdmin) {
            throw ForbiddenOperationException("Only admin users can suspend accounts")
        }

        val account =
            accountRepository.findById(input.targetAccountId)
                ?: throw AccountNotFoundException(input.targetAccountId.value)

        // version 不一致チェック（楽観ロック）
        if (account.version != input.version) {
            throw OptimisticLockException(input.targetAccountId.value, input.version, account.version)
        }

        if (!account.status.canTransitionTo(AccountStatus.SUSPENDED)) {
            throw InvalidStatusTransitionException(
                accountId = input.targetAccountId.value,
                from = account.status,
                to = AccountStatus.SUSPENDED,
            )
        }

        val suspended = account.suspend(updatedBy = input.operatorAccountId)
        val rows = accountRepository.update(suspended)
        if (rows == 0) {
            val currentVersion = accountRepository.findById(input.targetAccountId)?.version ?: -1
            throw OptimisticLockException(input.targetAccountId.value, input.version, currentVersion)
        }

        return Output(
            accountId = suspended.accountId.value,
            status = suspended.status,
            suspendedAt = checkNotNull(suspended.suspendedAt) { "suspendedAt must not be null after suspend()" },
            version = suspended.version,
        )
    }
}
