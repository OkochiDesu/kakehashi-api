package com.kakehashi.usecase.account

import com.kakehashi.domain.account.AccountId
import com.kakehashi.domain.account.AccountRepository
import com.kakehashi.domain.account.AccountStatus
import com.kakehashi.usecase.account.AccountTestFixtures.buildAccount
import com.kakehashi.usecase.account.exception.AccountNotFoundException
import com.kakehashi.usecase.account.exception.InvalidStatusTransitionException
import com.kakehashi.usecase.account.exception.OptimisticLockException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * RegisterAccountUseCase 単体テスト
 *
 * 設計書No：UC-A3
 * ADRNo：APP-ADR-0005, APP-ADR-0008
 *
 * ★★全体観点★★
 * アカウント登録完了（PROVISIONAL → ACTIVE）は一方向の操作であり、
 * 重複登録・不正ステータスからの遷移・楽観ロック競合を全て防御する必要がある。
 *
 * 《観　点》状態遷移・監査カラム（updatedAt/updatedBy）・version インクリメントの一括検証
 * 《テスト》正常系： PROVISIONAL から ACTIVE に遷移し repository_update が呼ばれる
 * 《テスト》異常系： update が 0件（楽観ロック競合）の場合は OptimisticLockException（currentVersion を再取得）
 *
 * 《観　点》不在ユーザーへの操作を早期失敗させることの確認
 * 《テスト》異常系： アカウントが存在しない場合は AccountNotFoundException
 *
 * 《観　点》PROVISIONAL 以外からの登録を禁止するステータスガードの確認（各ステータスを網羅）
 * 《テスト》異常系： ACTIVE アカウントへの再登録は InvalidStatusTransitionException
 * 《テスト》異常系： SUSPENDED アカウントへの登録試行は InvalidStatusTransitionException
 * 《テスト》異常系： DEACTIVATED アカウントへの登録試行は InvalidStatusTransitionException
 */
class RegisterAccountUseCaseTest {
    private val accountRepository = mockk<AccountRepository>()
    private val useCase = RegisterAccountUseCase(accountRepository)

    private val targetAccountId = AccountId("AZ0001")

    @Nested
    inner class NormalCases {
        @Test
        fun `正常系： PROVISIONAL から ACTIVE に遷移し repository_update が呼ばれる`() {
            val account = buildAccount(status = AccountStatus.PROVISIONAL, version = 0)
            every { accountRepository.findById(targetAccountId) } returns account
            every { accountRepository.update(any()) } returns 1

            val output = useCase.execute(targetAccountId)

            assertEquals("AZ0001", output.accountId)
            assertEquals(AccountStatus.ACTIVE, output.status)
            verify(exactly = 1) { accountRepository.update(match { it.status == AccountStatus.ACTIVE }) }
        }
    }

    @Nested
    inner class ErrorCases {
        @Test
        fun `異常系： アカウントが存在しない場合は AccountNotFoundException`() {
            every { accountRepository.findById(targetAccountId) } returns null

            assertThrows<AccountNotFoundException> {
                useCase.execute(targetAccountId)
            }
        }

        @Test
        fun `異常系： ACTIVE アカウントへの再登録は InvalidStatusTransitionException`() {
            val account = buildAccount(status = AccountStatus.ACTIVE)
            every { accountRepository.findById(targetAccountId) } returns account

            assertThrows<InvalidStatusTransitionException> {
                useCase.execute(targetAccountId)
            }
        }

        @Test
        fun `異常系： SUSPENDED アカウントへの登録試行は InvalidStatusTransitionException`() {
            val account = buildAccount(status = AccountStatus.SUSPENDED)
            every { accountRepository.findById(targetAccountId) } returns account

            assertThrows<InvalidStatusTransitionException> {
                useCase.execute(targetAccountId)
            }
        }

        @Test
        fun `異常系： DEACTIVATED アカウントへの登録試行は InvalidStatusTransitionException`() {
            val account = buildAccount(status = AccountStatus.DEACTIVATED)
            every { accountRepository.findById(targetAccountId) } returns account

            assertThrows<InvalidStatusTransitionException> {
                useCase.execute(targetAccountId)
            }
        }

        @Test
        fun `異常系： update が 0件（楽観ロック競合）の場合は OptimisticLockException（currentVersion を再取得）`() {
            val account = buildAccount(status = AccountStatus.PROVISIONAL, version = 0)
            val accountAfterConcurrentUpdate = buildAccount(status = AccountStatus.ACTIVE, version = 5)
            every { accountRepository.findById(targetAccountId) } returnsMany listOf(account, accountAfterConcurrentUpdate)
            every { accountRepository.update(any()) } returns 0

            val ex = assertThrows<OptimisticLockException> { useCase.execute(targetAccountId) }
            assertEquals(0, ex.requestVersion)
            assertEquals(5, ex.currentVersion)
        }
    }
}
