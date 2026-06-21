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
        fun `異常系： DEACTIVATED アカウントへの登録試行は InvalidStatusTransitionException`() {
            // SUSPENDED → ACTIVE は canTransitionTo が true のため InvalidStatusTransitionException にならない。
            // DEACTIVATED → ACTIVE は canTransitionTo が false のため InvalidStatusTransitionException になる。
            val account = buildAccount(status = AccountStatus.DEACTIVATED)
            every { accountRepository.findById(targetAccountId) } returns account

            assertThrows<InvalidStatusTransitionException> {
                useCase.execute(targetAccountId)
            }
        }

        @Test
        fun `異常系： update が 0件（楽観ロック競合）の場合は OptimisticLockException`() {
            val account = buildAccount(status = AccountStatus.PROVISIONAL, version = 0)
            every { accountRepository.findById(targetAccountId) } returns account
            // 0件更新 = 楽観ロック競合
            every { accountRepository.update(any()) } returns 0

            assertThrows<OptimisticLockException> {
                useCase.execute(targetAccountId)
            }
        }
    }
}
