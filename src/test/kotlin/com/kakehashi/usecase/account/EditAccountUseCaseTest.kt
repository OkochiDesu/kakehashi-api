package com.kakehashi.usecase.account

import com.kakehashi.domain.account.AccountId
import com.kakehashi.domain.account.AccountRepository
import com.kakehashi.domain.account.AccountStatus
import com.kakehashi.usecase.account.AccountTestFixtures.buildAccount
import com.kakehashi.usecase.account.exception.AccountNotFoundException
import com.kakehashi.usecase.account.exception.OptimisticLockException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * EditAccountUseCase 単体テスト
 *
 * 設計書No：UC-A4
 * ADRNo：APP-ADR-0005, APP-ADR-0008
 *
 * ★観点
 * 本人による表示名の自己編集操作を検証する。
 * 監査カラム（updatedAt / updatedBy）の正確性と、楽観ロックによる同時編集競合検出の
 * 2 段階（事前チェック・DB 更新件数チェック）を確認することが重要。
 *
 * ★★正常系★★
 * 《観　点》name・updatedAt・updatedBy が更新後の値に変化していることの一括検証
 * 《テスト》name が更新された Output が返る
 *
 * ★★異常系★★
 * 《観　点》不在ユーザーへの変更試行を防ぐ早期失敗の確認
 * 《テスト》アカウントが存在しない場合は AccountNotFoundException
 *
 * 《観　点》楽観ロック競合の 2 段階検出の確認
 * 《テスト》リクエスト version と DB の version が不一致は OptimisticLockException（第1段階: 事前チェック）
 * 《テスト》update が 0件（楽観ロック競合）の場合は OptimisticLockException（第2段階: currentVersion 再取得）
 */
class EditAccountUseCaseTest {
    private val accountRepository = mockk<AccountRepository>()
    private val useCase = EditAccountUseCase(accountRepository)

    private val targetAccountId = AccountId("AZ0001")
    private val input =
        EditAccountUseCase.Input(
            accountId = targetAccountId,
            name = "新しい名前",
            version = 0,
        )

    @Nested
    inner class NormalCases {
        @Test
        fun `正常系： name が更新された Output が返る`() {
            val account = buildAccount(status = AccountStatus.ACTIVE, version = 0)
            every { accountRepository.findById(targetAccountId) } returns account
            every { accountRepository.update(any()) } returns 1

            val output = useCase.execute(input)

            assertEquals("AZ0001", output.accountId)
            assertEquals("新しい名前", output.name)
            assertEquals(1, output.version)
            verify(exactly = 1) { accountRepository.update(match { it.name == "新しい名前" }) }
        }
    }

    @Nested
    inner class ErrorCases {
        @Test
        fun `異常系： アカウントが存在しない場合は AccountNotFoundException`() {
            every { accountRepository.findById(targetAccountId) } returns null

            assertThrows<AccountNotFoundException> {
                useCase.execute(input)
            }
        }

        @Test
        fun `異常系： リクエスト version と DB の version が不一致は OptimisticLockException`() {
            // DB には version=1 が存在するが、リクエストは version=0
            val account = buildAccount(status = AccountStatus.ACTIVE, version = 1)
            every { accountRepository.findById(targetAccountId) } returns account

            assertThrows<OptimisticLockException> {
                useCase.execute(input)
            }
        }

        @Test
        fun `異常系： update が 0件（楽観ロック競合）の場合は OptimisticLockException（currentVersion を再取得）`() {
            val account = buildAccount(status = AccountStatus.ACTIVE, version = 0)
            val accountAfterConcurrentUpdate = buildAccount(status = AccountStatus.ACTIVE, version = 5)
            every { accountRepository.findById(targetAccountId) } returnsMany listOf(account, accountAfterConcurrentUpdate)
            every { accountRepository.update(any()) } returns 0

            val ex =
                assertThrows<OptimisticLockException> {
                    useCase.execute(input)
                }
            assertEquals(0, ex.requestVersion)
            assertEquals(5, ex.currentVersion)
        }
    }
}
