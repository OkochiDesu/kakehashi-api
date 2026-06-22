package com.kakehashi.usecase.account

import com.kakehashi.domain.account.AccountId
import com.kakehashi.domain.account.AccountRepository
import com.kakehashi.domain.account.AccountStatus
import com.kakehashi.usecase.account.exception.AccountNotFoundException
import com.kakehashi.usecase.account.exception.InvalidStatusTransitionException
import com.kakehashi.usecase.account.exception.OptimisticLockException

/**
 * UC-A3: 本登録申込み UseCase
 *
 * 根拠: docs/design/api/account-role.md（UC-A3）
 * - status = 'provisional' のアカウントのみ実行可能
 * - 自動的に status を provisional → active へ遷移させる（管理者承認なし）
 * - デフォルト権限付与はなし（APP-ADR-0007 決定4）
 * - version による楽観ロック（APP-ADR-0005）
 */
class RegisterAccountUseCase(
    private val accountRepository: AccountRepository,
) {
    data class Output(
        val accountId: String,
        val status: AccountStatus,
    )

    /**
     * 本登録申込みを行い、PROVISIONAL → ACTIVE へ遷移させる。
     *
     * 設計書No：UC-A3
     * ADRNo：APP-ADR-0005, APP-ADR-0008
     *
     * @param accountId 本登録対象のアカウントID
     * @throws AccountNotFoundException アカウントが存在しない場合
     * @throws InvalidStatusTransitionException PROVISIONAL 以外のステータスから呼んだ場合（二重申込み防止）
     * @throws OptimisticLockException 楽観ロック競合の場合
     */
    fun execute(accountId: AccountId): Output {
        val account =
            accountRepository.findById(accountId)
                ?: throw AccountNotFoundException(accountId.value)

        // UC-A3: PROVISIONAL のみ実行可能（ACTIVE/SUSPENDED/DEACTIVATED は 409）
        if (account.status != AccountStatus.PROVISIONAL) {
            throw InvalidStatusTransitionException(
                accountId = accountId.value,
                from = account.status,
                to = AccountStatus.ACTIVE,
            )
        }

        val registered = account.register(updatedBy = accountId.value)
        val updated = accountRepository.update(registered)
        if (updated == 0) {
            val currentVersion = accountRepository.findById(accountId)?.version ?: -1
            throw OptimisticLockException(accountId.value, account.version, currentVersion)
        }

        return Output(
            accountId = registered.accountId.value,
            status = registered.status,
        )
    }
}
