package com.kakehashi.infrastructure.account

import com.kakehashi.domain.account.Account
import com.kakehashi.domain.account.AccountId
import com.kakehashi.domain.account.AccountStatus
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.OffsetDateTime
import java.util.UUID

/**
 * AccountRepositoryImpl 単体テスト
 *
 * 設計書No：UC-A6
 * ADRNo：APP-ADR-0005, APP-ADR-0016
 *
 * ★★全体観点★★
 * `assignRolesAndBumpVersion` の引数間（`accountId`/`account.accountId`、`operatorId`/`account.updatedBy`）
 * の整合性検証を保証する。不一致のまま呼び出すと、別アカウントの version を更新した後に別アカウントの
 * account_roles を書き換える、あるいは accounts と account_roles で監査カラムの操作者が食い違う
 * データ不整合が起こり得るため（PR #23 Copilot レビュー指摘）、DB 呼び出し前に検証で防ぐ。
 *
 * 《観　点》assignRolesAndBumpVersion の accountId 整合性検証
 * 《テスト》異常系： accountIdとaccountパラメータのaccountIdが不一致だとIllegalArgumentExceptionをスローする
 *
 * 《観　点》assignRolesAndBumpVersion の operatorId 整合性検証
 * 《テスト》異常系： operatorIdとaccountのupdatedByが不一致だとIllegalArgumentExceptionをスローする
 */
class AccountRepositoryImplTest {
    @Test
    fun `異常系： accountIdとaccountパラメータのaccountIdが不一致だとIllegalArgumentExceptionをスローする`() {
        val accountMapper = mockk<AccountMapper>()
        val repository = AccountRepositoryImpl(accountMapper)
        val account = buildAccount("AZ0001")

        assertThrows<IllegalArgumentException> {
            repository.assignRolesAndBumpVersion(
                accountId = AccountId("AZ0002"),
                roleIds = listOf(UUID.randomUUID()),
                account = account,
                operatorId = "AZ0002",
            )
        }
        verify(exactly = 0) { accountMapper.updateAccountRow(any(), any()) }
        verify(exactly = 0) { accountMapper.deleteAccountRoles(any()) }
    }

    @Test
    fun `異常系： operatorIdとaccountのupdatedByが不一致だとIllegalArgumentExceptionをスローする`() {
        val accountMapper = mockk<AccountMapper>()
        val repository = AccountRepositoryImpl(accountMapper)
        val account = buildAccount("AZ0001", updatedBy = "AZ0001")

        assertThrows<IllegalArgumentException> {
            repository.assignRolesAndBumpVersion(
                accountId = AccountId("AZ0001"),
                roleIds = listOf(UUID.randomUUID()),
                account = account,
                operatorId = "ATTACKER",
            )
        }
        verify(exactly = 0) { accountMapper.updateAccountRow(any(), any()) }
        verify(exactly = 0) { accountMapper.deleteAccountRoles(any()) }
    }

    private fun buildAccount(
        accountId: String,
        updatedBy: String = accountId,
    ): Account =
        Account.reconstruct(
            accountId = AccountId(accountId),
            googleSubHash = "hash_$accountId",
            email = "user@example.com",
            name = "テストユーザー",
            status = AccountStatus.ACTIVE,
            suspendedAt = null,
            version = 0,
            createdBy = accountId,
            updatedBy = updatedBy,
            createdAt = OffsetDateTime.parse("2026-01-01T00:00:00+09:00"),
            updatedAt = OffsetDateTime.parse("2026-01-01T00:00:00+09:00"),
        )
}
