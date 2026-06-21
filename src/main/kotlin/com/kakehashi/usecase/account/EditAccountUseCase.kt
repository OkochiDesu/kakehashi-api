package com.kakehashi.usecase.account

import com.kakehashi.domain.account.AccountId
import com.kakehashi.domain.account.AccountRepository
import com.kakehashi.domain.account.AccountStatus
import com.kakehashi.usecase.account.exception.AccountNotFoundException
import com.kakehashi.usecase.account.exception.OptimisticLockException

/**
 * UC-A4: アカウント情報編集（本人）UseCase
 *
 * 根拠: docs/design/api/account-role.md（UC-A4）
 * - 本人が自分の表示名（accounts.name）を編集する
 * - APP-ADR-0005: version による楽観ロック。version 不一致は 409 Conflict
 */
class EditAccountUseCase(
    private val accountRepository: AccountRepository,
) {
    data class Input(
        val accountId: AccountId,
        val name: String,
        val version: Int,
    )

    data class Output(
        val accountId: String,
        val name: String,
        val email: String,
        val status: AccountStatus,
        val version: Int,
    )

    /**
     * アカウントの表示名を更新する。
     *
     * 設計書No：UC-A4
     * ADRNo：APP-ADR-0005, APP-ADR-0008
     *
     * @param input accountId・新しい name・version を含む入力値
     * @throws AccountNotFoundException アカウントが存在しない場合
     * @throws OptimisticLockException version 不一致または DB 更新 0件の場合
     */
    fun execute(input: Input): Output {
        val account =
            accountRepository.findById(input.accountId)
                ?: throw AccountNotFoundException(input.accountId.value)

        // version 不一致チェック（楽観ロック）
        if (account.version != input.version) {
            throw OptimisticLockException(input.accountId.value, input.version, account.version)
        }

        val updated = account.editName(name = input.name, updatedBy = input.accountId.value)
        val rows = accountRepository.update(updated)
        if (rows == 0) {
            val currentVersion = accountRepository.findById(input.accountId)?.version ?: -1
            throw OptimisticLockException(input.accountId.value, input.version, currentVersion)
        }

        return Output(
            accountId = updated.accountId.value,
            name = updated.name,
            email = updated.email,
            status = updated.status,
            version = updated.version,
        )
    }
}
