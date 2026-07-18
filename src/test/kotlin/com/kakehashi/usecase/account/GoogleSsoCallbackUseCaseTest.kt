package com.kakehashi.usecase.account

import com.kakehashi.domain.account.Account
import com.kakehashi.domain.account.AccountId
import com.kakehashi.domain.account.AccountRepository
import com.kakehashi.domain.account.AccountStatus
import com.kakehashi.domain.account.GoogleIdTokenVerificationFailedException
import com.kakehashi.domain.account.GoogleIdTokenVerifier
import com.kakehashi.domain.account.GoogleIdentity
import com.kakehashi.domain.account.JwtTokenIssuer
import com.kakehashi.infrastructure.account.JwtTokenIssuerImpl
import com.kakehashi.usecase.account.AccountTestFixtures.buildAccount
import com.kakehashi.usecase.account.exception.DomainNotAllowedException
import com.kakehashi.usecase.account.exception.ForbiddenOperationException
import com.kakehashi.usecase.account.exception.GoogleIdTokenVerificationException
import com.kakehashi.usecase.account.exception.InvalidIdTokenFormatException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * GoogleSsoCallbackUseCase 単体テスト
 *
 * 設計書No：UC-A1
 * ADRNo：APP-ADR-0014
 *
 * ★★全体観点★★
 * Google SSO ログインの3段構成（Google ID トークン検証 → JIT プロビジョニング → 自前JWT発行）が
 * 仕様通りに統合されていることを保証する。特に suspended/deactivated アカウントで JWT が
 * 発行されないこと（ログイン拒否）は認証基盤の根幹であり、最重要観点として扱う。
 *
 * 《観　点》idToken のフォーマット検証（JWT フォーマットチェックが Google 検証より先に行われることの確認）
 * 《テスト》異常系： idToken が JWT フォーマットとして不正な場合は InvalidIdTokenFormatException（Google検証は呼ばれない）
 *
 * 《観　点》Google ID トークン検証失敗時にアカウント・JWTとも作成されないことの確認
 * 《テスト》異常系： Google JWKS 署名検証に失敗した場合は GoogleIdTokenVerificationException（アカウント未作成）
 *
 * 《観　点》許可ドメイン外の Google アカウントを拒否することの確認
 * 《テスト》異常系： 許可された会社ドメイン以外のメールアドレスは DomainNotAllowedException
 *
 * 《観　点》初回 Google ログイン時に JIT プロビジョニングで自動アカウント登録・自前JWT発行が行われることの確認
 * 《テスト》正常系： 未登録アカウントは JIT プロビジョニングで PROVISIONAL 作成され自前JWTが発行される
 * 《テスト》正常系： 発行された自前JWTに accountId クレームが含まれる
 *
 * 《観　点》2回目以降のログインで再作成が起きず既存ステータスに応じて自前JWTが発行されることの確認
 * 《テスト》正常系： 既存 PROVISIONAL アカウントは再作成されず自前JWTが発行される
 * 《テスト》正常系： 既存 ACTIVE アカウントは自前JWTを発行しマイページへ誘導する
 *
 * 《観　点》ログイン不可ステータス（suspended/deactivated）のアカウントを拒否することの確認
 * 《テスト》異常系： 既存 SUSPENDED アカウントは ForbiddenOperationException（JWT未発行）
 * 《テスト》異常系： 既存 DEACTIVATED アカウントは ForbiddenOperationException（JWT未発行）
 */
class GoogleSsoCallbackUseCaseTest {
    private val accountRepository = mockk<AccountRepository>()
    private val googleIdTokenVerifier = mockk<GoogleIdTokenVerifier>()
    private val jwtTokenIssuer = mockk<JwtTokenIssuer>()

    private val useCase =
        GoogleSsoCallbackUseCase(
            accountRepository = accountRepository,
            googleIdTokenVerifier = googleIdTokenVerifier,
            jwtTokenIssuer = jwtTokenIssuer,
            allowedGoogleDomains = emptySet(),
        )

    // JWT フォーマットチェック（3 セグメント）を満たすダミートークン。実際のデコードは googleIdTokenVerifier のモックが担う。
    private val validFormatToken = "header.payload.signature"

    private val identity =
        GoogleIdentity(
            googleSubHash = "new_hash",
            email = "new@example.com",
            name = "新規ユーザー",
        )

    private fun input(idToken: String = validFormatToken) = GoogleSsoCallbackUseCase.Input(idToken = idToken)

    @Test
    fun `異常系： idToken が JWT フォーマットとして不正な場合は InvalidIdTokenFormatException（Google検証は呼ばれない）`() {
        assertThrows(InvalidIdTokenFormatException::class.java) {
            useCase.execute(input(idToken = "not-a-jwt"))
        }
        verify(exactly = 0) { googleIdTokenVerifier.verify(any()) }
    }

    @Test
    fun `異常系： Google JWKS 署名検証に失敗した場合は GoogleIdTokenVerificationException（アカウント未作成）`() {
        every { googleIdTokenVerifier.verify(validFormatToken) } throws
            GoogleIdTokenVerificationFailedException("署名が不正です")

        assertThrows(GoogleIdTokenVerificationException::class.java) {
            useCase.execute(input())
        }
        // jwtTokenIssuer は未スタブのため、想定外に呼ばれた場合は MockKException が発生し
        // 上記 assertThrows の期待する例外型と一致せずテストが失敗する（呼ばれないことの検証を兼ねる）。
        verify(exactly = 0) { accountRepository.save(any()) }
    }

    @Test
    fun `異常系： 許可された会社ドメイン以外のメールアドレスは DomainNotAllowedException`() {
        val restrictedUseCase =
            GoogleSsoCallbackUseCase(
                accountRepository = accountRepository,
                googleIdTokenVerifier = googleIdTokenVerifier,
                jwtTokenIssuer = jwtTokenIssuer,
                allowedGoogleDomains = setOf("allowed.example.com"),
            )
        every { googleIdTokenVerifier.verify(validFormatToken) } returns
            identity.copy(email = "user@other-domain.com")

        assertThrows(DomainNotAllowedException::class.java) {
            restrictedUseCase.execute(input())
        }
        // jwtTokenIssuer は未スタブのため、想定外に呼ばれた場合は MockKException が発生し
        // 上記 assertThrows の期待する例外型と一致せずテストが失敗する（呼ばれないことの検証を兼ねる）。
        verify(exactly = 0) { accountRepository.save(any()) }
    }

    @Nested
    inner class JitProvisioning {
        @Test
        fun `正常系： 未登録アカウントは JIT プロビジョニングで PROVISIONAL 作成され自前JWTが発行される`() {
            every { googleIdTokenVerifier.verify(validFormatToken) } returns identity
            every { accountRepository.findByGoogleSubHash(identity.googleSubHash) } returns null
            every { accountRepository.nextAccountIdSequence() } returns 1L
            val savedSlot = slot<Account>()
            every { accountRepository.save(capture(savedSlot)) } returns Unit
            every { jwtTokenIssuer.issue(AccountId("AZ0001")) } returns "signed.jwt.token"

            val output = useCase.execute(input())

            assertEquals("AZ0001", output.accountId)
            assertEquals(AccountStatus.PROVISIONAL, output.status)
            assertEquals("/registration", output.redirectTo)
            assertEquals("signed.jwt.token", output.accessToken)
            verify(exactly = 1) { accountRepository.save(any()) }

            val saved = savedSlot.captured
            assertEquals("AZ0001", saved.createdBy)
            assertEquals("AZ0001", saved.updatedBy)
            assertEquals(0, saved.version)
        }
    }

    @Test
    fun `正常系： 発行された自前JWTに accountId クレームが含まれる`() {
        // 実際の JWT エンコード・デコードを検証するため JwtTokenIssuerImpl を実体で使用する
        val secret = "test-only-jwt-secret-for-unit-test-min-32-bytes"
        val realJwtTokenIssuer = JwtTokenIssuerImpl(secret = secret, expirationSeconds = 3600)
        val useCaseWithRealIssuer =
            GoogleSsoCallbackUseCase(
                accountRepository = accountRepository,
                googleIdTokenVerifier = googleIdTokenVerifier,
                jwtTokenIssuer = realJwtTokenIssuer,
                allowedGoogleDomains = emptySet(),
            )
        every { googleIdTokenVerifier.verify(validFormatToken) } returns identity
        every { accountRepository.findByGoogleSubHash(identity.googleSubHash) } returns null
        every { accountRepository.nextAccountIdSequence() } returns 5L
        every { accountRepository.save(any()) } returns Unit

        val output = useCaseWithRealIssuer.execute(input())

        val claims =
            Jwts
                .parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8)))
                .build()
                .parseSignedClaims(output.accessToken)
                .payload
        assertEquals("AZ0005", claims.get("accountId", String::class.java))
    }

    @Nested
    inner class ExistingAccount {
        @Test
        fun `正常系： 既存 PROVISIONAL アカウントは再作成されず自前JWTが発行される`() {
            val existing = buildAccount(accountId = "AZ0004", status = AccountStatus.PROVISIONAL)
            every { googleIdTokenVerifier.verify(validFormatToken) } returns identity
            every { accountRepository.findByGoogleSubHash(identity.googleSubHash) } returns existing
            every { jwtTokenIssuer.issue(AccountId("AZ0004")) } returns "signed.jwt.token"

            val output = useCase.execute(input())

            assertEquals("AZ0004", output.accountId)
            assertEquals(AccountStatus.PROVISIONAL, output.status)
            assertEquals("/registration", output.redirectTo)
            assertEquals("signed.jwt.token", output.accessToken)
            verify(exactly = 0) { accountRepository.save(any()) }
        }

        @Test
        fun `正常系： 既存 ACTIVE アカウントは自前JWTを発行しマイページへ誘導する`() {
            val existing = buildAccount(accountId = "AZ0002", status = AccountStatus.ACTIVE)
            every { googleIdTokenVerifier.verify(validFormatToken) } returns identity
            every { accountRepository.findByGoogleSubHash(identity.googleSubHash) } returns existing
            every { jwtTokenIssuer.issue(AccountId("AZ0002")) } returns "signed.jwt.token"

            val output = useCase.execute(input())

            assertEquals("AZ0002", output.accountId)
            assertEquals(AccountStatus.ACTIVE, output.status)
            assertEquals("/mypage", output.redirectTo)
            assertEquals("signed.jwt.token", output.accessToken)
            verify(exactly = 0) { accountRepository.save(any()) }
        }

        @Test
        fun `異常系： 既存 SUSPENDED アカウントは ForbiddenOperationException（JWT未発行）`() {
            val existing = buildAccount(accountId = "AZ0003", status = AccountStatus.SUSPENDED, version = 2)
            every { googleIdTokenVerifier.verify(validFormatToken) } returns identity
            every { accountRepository.findByGoogleSubHash(identity.googleSubHash) } returns existing

            assertThrows(ForbiddenOperationException::class.java) {
                useCase.execute(input())
            }
            // AccountId は @JvmInline value class のため any() でのシグネチャ生成に失敗する。
            // 具体値で「その accountId に対して呼ばれていないこと」を検証する。
            verify(exactly = 0) { jwtTokenIssuer.issue(AccountId("AZ0003")) }
            verify(exactly = 0) { accountRepository.save(any()) }
        }

        @Test
        fun `異常系： 既存 DEACTIVATED アカウントは ForbiddenOperationException（JWT未発行）`() {
            val existing = buildAccount(accountId = "AZ0005", status = AccountStatus.DEACTIVATED, version = 3)
            every { googleIdTokenVerifier.verify(validFormatToken) } returns identity
            every { accountRepository.findByGoogleSubHash(identity.googleSubHash) } returns existing

            assertThrows(ForbiddenOperationException::class.java) {
                useCase.execute(input())
            }
            // AccountId は @JvmInline value class のため any() でのシグネチャ生成に失敗する。
            // 具体値で「その accountId に対して呼ばれていないこと」を検証する。
            verify(exactly = 0) { jwtTokenIssuer.issue(AccountId("AZ0005")) }
            verify(exactly = 0) { accountRepository.save(any()) }
        }
    }
}
