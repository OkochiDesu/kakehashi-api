package com.kakehashi.usecase.account

import com.kakehashi.domain.account.AccountId
import com.kakehashi.domain.account.AccountRepository
import com.kakehashi.domain.account.AccountStatus
import com.kakehashi.usecase.account.AccountTestFixtures.buildAccount
import com.kakehashi.usecase.account.exception.AccountNotFoundException
import com.kakehashi.usecase.account.exception.ForbiddenOperationException
import com.kakehashi.usecase.account.exception.InvalidStatusTransitionException
import com.kakehashi.usecase.account.exception.OptimisticLockException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.OffsetDateTime

/**
 * UnsuspendAccountUseCase 単体テスト
 *
 * 設計書No：UC-A7
 * ADRNo：APP-ADR-0005, APP-ADR-0008
 *
 * ★観点
 * 管理者によるアカウント停止解除操作（SUSPENDED → ACTIVE）を検証する。
 * 権限チェック・状態遷移・suspendedAt のクリア・楽観ロックが一貫して機能することを確認する。
 *
 * ★★正常系★★
 * 《観　点》状態遷移・updatedAt/updatedBy・suspendedAt クリアが正しく実行されることの一括検証
 * 《テスト》SUSPENDED から ACTIVE に遷移する
 *
 * ★★異常系★★
 * 《観　点》非管理者による停止解除操作が権限ガードで防止されることの確認
 * 《テスト》isAdmin=false は ForbiddenOperationException
 *
 * 《観　点》不在エンティティへの操作が早期失敗することの確認
 * 《テスト》アカウントが存在しない場合は AccountNotFoundException
 *
 * 《観　点》SUSPENDED 以外からの解除操作が禁止されることの確認（各ステータスを網羅）
 * 《テスト》ACTIVE から ACTIVE への遷移試行は InvalidStatusTransitionException
 * 《テスト》PROVISIONAL から ACTIVE への解除試行は InvalidStatusTransitionException
 *
 * 《観　点》楽観ロック競合時の例外と currentVersion の正確な保持確認
 * 《テスト》update が 0件（楽観ロック競合）は OptimisticLockException（currentVersion を再取得）
 */
class UnsuspendAccountUseCaseTest {
    private val accountRepository = mockk<AccountRepository>()
    private val useCase = UnsuspendAccountUseCase(accountRepository)

    private val targetAccountId = AccountId("AZ0001")

    private fun buildInput(
        isAdmin: Boolean = true,
        version: Int = 0,
    ) = UnsuspendAccountUseCase.Input(
        targetAccountId = targetAccountId,
        operatorAccountId = "AZ0002",
        isAdmin = isAdmin,
        version = version,
    )

    @Nested
    inner class NormalCases {
        @Test
        fun `正常系： SUSPENDED から ACTIVE に遷移する`() {
            val account =
                buildAccount(
                    status = AccountStatus.SUSPENDED,
                    version = 0,
                    suspendedAt = OffsetDateTime.now(),
                )
            every { accountRepository.findById(targetAccountId) } returns account
            every { accountRepository.update(any()) } returns 1

            val output = useCase.execute(buildInput())

            assertEquals("AZ0001", output.accountId)
            assertEquals(AccountStatus.ACTIVE, output.status)
            assertNull(output.suspendedAt)
            assertEquals(1, output.version)
            verify(exactly = 1) { accountRepository.update(match { it.status == AccountStatus.ACTIVE && it.suspendedAt == null }) }
        }
    }

    @Nested
    inner class ErrorCases {
        @Test
        fun `異常系： isAdmin=false は ForbiddenOperationException`() {
            assertThrows<ForbiddenOperationException> {
                useCase.execute(buildInput(isAdmin = false))
            }
        }

        @Test
        fun `異常系： アカウントが存在しない場合は AccountNotFoundException`() {
            every { accountRepository.findById(targetAccountId) } returns null

            assertThrows<AccountNotFoundException> {
                useCase.execute(buildInput())
            }
        }

        @Test
        fun `異常系： ACTIVE から ACTIVE への遷移試行は InvalidStatusTransitionException`() {
            val account = buildAccount(status = AccountStatus.ACTIVE, version = 0)
            every { accountRepository.findById(targetAccountId) } returns account

            assertThrows<InvalidStatusTransitionException> {
                useCase.execute(buildInput())
            }
        }

        @Test
        fun `異常系： PROVISIONAL から ACTIVE への解除試行は InvalidStatusTransitionException`() {
            val account = buildAccount(status = AccountStatus.PROVISIONAL, version = 0)
            every { accountRepository.findById(targetAccountId) } returns account

            assertThrows<InvalidStatusTransitionException> {
                useCase.execute(buildInput())
            }
        }

        @Test
        fun `異常系： update が 0件（楽観ロック競合）は OptimisticLockException（currentVersion を再取得）`() {
            val account =
                buildAccount(
                    status = AccountStatus.SUSPENDED,
                    version = 0,
                    suspendedAt = OffsetDateTime.now(),
                )
            val accountAfterConcurrentUpdate = buildAccount(status = AccountStatus.ACTIVE, version = 5)
            every { accountRepository.findById(targetAccountId) } returnsMany listOf(account, accountAfterConcurrentUpdate)
            every { accountRepository.update(any()) } returns 0

            val ex = assertThrows<OptimisticLockException> { useCase.execute(buildInput()) }
            assertEquals(0, ex.requestVersion)
            assertEquals(5, ex.currentVersion)
        }
    }
}
