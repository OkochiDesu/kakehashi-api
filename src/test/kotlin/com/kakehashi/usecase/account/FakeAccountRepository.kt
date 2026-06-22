package com.kakehashi.usecase.account

import com.kakehashi.domain.account.Account
import com.kakehashi.domain.account.AccountId
import com.kakehashi.domain.account.AccountRepository
import java.util.UUID

/**
 * テスト用 AccountRepository フェイク実装
 *
 * MockK の @JvmInline value class (AccountId) サポートが不完全なため、
 * interface の fake 実装を使用してシグネチャ生成エラーを回避する。
 */
class FakeAccountRepository : AccountRepository {
    /** テスト用の accounts ストア */
    val accounts = mutableMapOf<String, Account>()

    /** findById の呼び出し履歴 */
    val findByIdCalls = mutableListOf<AccountId>()

    /** update の呼び出し履歴（戻り値を制御可能） */
    var updateResult: Int = 1

    /** assignRolesAndBumpVersion の戻り値を制御可能 */
    var assignRolesAndBumpVersionResult: Int = 1

    /** assignRolesAndBumpVersion に渡された引数を記録 */
    val assignRolesAndBumpVersionCalls = mutableListOf<Triple<AccountId, List<UUID>, String>>()

    override fun findById(accountId: AccountId): Account? {
        findByIdCalls.add(accountId)
        return accounts[accountId.value]
    }

    override fun findByGoogleSubHash(googleSubHash: String): Account? = accounts.values.firstOrNull { it.googleSubHash == googleSubHash }

    override fun save(account: Account) {
        accounts[account.accountId.value] = account
    }

    override fun update(account: Account): Int {
        if (updateResult > 0) {
            accounts[account.accountId.value] = account
        }
        return updateResult
    }

    override fun nextAccountIdSequence(): Long = (accounts.size + 1).toLong()

    override fun findRoleIdsByAccountId(accountId: AccountId): Set<UUID> = emptySet()

    override fun assignRolesAndBumpVersion(
        accountId: AccountId,
        roleIds: List<UUID>,
        account: Account,
        operatorId: String,
    ): Int {
        assignRolesAndBumpVersionCalls.add(Triple(accountId, roleIds, operatorId))
        if (assignRolesAndBumpVersionResult > 0) {
            accounts[account.accountId.value] = account
        }
        return assignRolesAndBumpVersionResult
    }
}
