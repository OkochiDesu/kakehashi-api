package com.kakehashi.usecase.account

import com.kakehashi.domain.account.AccountRepository
import com.kakehashi.domain.account.AccountStatus
import com.kakehashi.usecase.account.AccountTestFixtures.buildAccount
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * GoogleSsoCallbackUseCase 単体テスト
 *
 * 設計書No：UC-A1
 * ADRNo：APP-ADR-0008
 *
 * ★観点
 * Google SSO コールバックの責務は「アカウントの存在確認と JIT 作成のみ」であり、
 * アクセス制御（SUSPENDED 拒否等）は Controller 層が担う。
 * この責務分離が正しいことを保証する（異常系ケースが存在しないのはこの理由による）。
 *
 * ★★正常系★★
 * 《観　点》初回 Google ログイン時に JIT プロビジョニングで自動アカウント登録が行われることの確認
 * 《テスト》未登録アカウントは JIT プロビジョニングで PROVISIONAL 作成される
 *
 * 《観　点》2回目以降のログインで再作成が起きず既存ステータスがそのまま返ることの確認
 * 《テスト》既存 ACTIVE アカウントはそのまま返す
 * 《テスト》既存 SUSPENDED アカウントは SUSPENDED ステータスを返す（コールバックでは弾かない）
 * 《テスト》既存 PROVISIONAL アカウントは PROVISIONAL ステータスを返す
 *
 * 《観　点》新規アカウントの accountId が DB シーケンス経由で採番されることの確認
 * 《テスト》新規アカウントの accountId はシーケンス値から生成される
 */
class GoogleSsoCallbackUseCaseTest {
    private val accountRepository = mockk<AccountRepository>()
    private val useCase = GoogleSsoCallbackUseCase(accountRepository)

    private val input =
        GoogleSsoCallbackUseCase.Input(
            googleSubHash = "new_hash",
            email = "new@example.com",
            name = "新規ユーザー",
        )

    @Nested
    inner class JitProvisioning {
        @Test
        fun `正常系： 未登録アカウントは JIT プロビジョニングで PROVISIONAL 作成される`() {
            every { accountRepository.findByGoogleSubHash(any()) } returns null
            every { accountRepository.nextAccountIdSequence() } returns 1L
            every { accountRepository.save(any()) } returns Unit

            val output = useCase.execute(input)

            assertEquals("AZ0001", output.accountId)
            assertEquals(AccountStatus.PROVISIONAL, output.status)
            assertEquals("/registration", output.redirectTo)
            verify(exactly = 1) { accountRepository.save(any()) }
        }
    }

    @Nested
    inner class ExistingAccount {
        @Test
        fun `正常系： 既存 ACTIVE アカウントはそのまま返す`() {
            val existing = buildAccount(accountId = "AZ0002", status = AccountStatus.ACTIVE)
            every { accountRepository.findByGoogleSubHash(any()) } returns existing

            val output = useCase.execute(input)

            assertEquals("AZ0002", output.accountId)
            assertEquals(AccountStatus.ACTIVE, output.status)
            assertEquals("/mypage", output.redirectTo)
            verify(exactly = 0) { accountRepository.save(any()) }
        }

        @Test
        fun `正常系： 既存 SUSPENDED アカウントは SUSPENDED ステータスを返す（コールバックでは弾かない）`() {
            val existing = buildAccount(accountId = "AZ0003", status = AccountStatus.SUSPENDED)
            every { accountRepository.findByGoogleSubHash(any()) } returns existing

            // コールバック UseCase 自体はアカウントを返す（Controller 層で 403 に変換）
            val output = useCase.execute(input)

            assertEquals("AZ0003", output.accountId)
            assertEquals(AccountStatus.SUSPENDED, output.status)
            assertEquals("/error/suspended", output.redirectTo)
        }

        @Test
        fun `正常系： 既存 PROVISIONAL アカウントは PROVISIONAL ステータスを返す`() {
            val existing = buildAccount(accountId = "AZ0004", status = AccountStatus.PROVISIONAL)
            every { accountRepository.findByGoogleSubHash(any()) } returns existing

            val output = useCase.execute(input)

            assertEquals(AccountStatus.PROVISIONAL, output.status)
            assertEquals("/registration", output.redirectTo)
        }
    }

    @Test
    fun `正常系： 新規アカウントの accountId はシーケンス値から生成される`() {
        every { accountRepository.findByGoogleSubHash(any()) } returns null
        every { accountRepository.nextAccountIdSequence() } returns 99L
        every { accountRepository.save(any()) } returns Unit

        val output = useCase.execute(input)

        assertEquals("AZ0099", output.accountId)
    }
}
