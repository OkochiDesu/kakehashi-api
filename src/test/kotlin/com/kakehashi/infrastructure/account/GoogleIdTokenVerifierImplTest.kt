package com.kakehashi.infrastructure.account

import com.kakehashi.domain.account.GoogleIdTokenVerificationFailedException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.security.MessageDigest
import java.time.Instant

/**
 * GoogleIdTokenVerifierImpl 単体テスト
 *
 * 設計書No：UC-A1
 * ADRNo：APP-ADR-0014
 *
 * ★★全体観点★★
 * デコード済み Google ID トークン（[Jwt]）からアカウント情報を組み立てる [GoogleIdTokenVerifierImpl.extractIdentity]
 * を検証する。`email_verified` を検証せずに `email` を信頼すると、メール未確認の Google アカウントで
 * ドメイン制限（`allowed-domains`）をすり抜けられるため、この検証は認証基盤のセキュリティ根幹を担う。
 * NimbusJwtDecoder による実際の署名検証はネットワーク疎通を要するため対象外（exec-plan 0006 引き継ぎ事項）。
 *
 * 《観　点》email_verified クレームの検証
 * 《テスト》正常系： email_verified=true の場合は GoogleIdentity が生成される
 * 《テスト》異常系： email_verified=false の場合は GoogleIdTokenVerificationFailedException
 * 《テスト》異常系： email_verified クレームが存在しない場合は GoogleIdTokenVerificationFailedException
 *
 * 《観　点》必須クレーム欠落の検証
 * 《テスト》異常系： sub クレームが存在しない場合は GoogleIdTokenVerificationFailedException
 * 《テスト》異常系： email クレームが存在しない場合は GoogleIdTokenVerificationFailedException
 *
 * 《観　点》name クレーム欠落時のフォールバック
 * 《テスト》正常系： name クレームが存在しない場合は email が name として使われる
 */
class GoogleIdTokenVerifierImplTest {
    private val verifier =
        GoogleIdTokenVerifierImpl(
            jwksUri = "https://example.com/jwks",
            issuer = "https://accounts.google.com",
            clientId = "test-client-id",
        )

    private fun jwtBuilder(): Jwt.Builder =
        Jwt
            .withTokenValue("dummy-token")
            .header("alg", "RS256")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }

    @Test
    fun `正常系： email_verified=true の場合は GoogleIdentity が生成される`() {
        val jwt =
            jwtBuilder()
                .subject("google-sub-123")
                .claim("email", "user@example.com")
                .claim("email_verified", true)
                .claim("name", "テストユーザー")
                .build()

        val identity = verifier.extractIdentity(jwt)

        assertEquals(sha256("google-sub-123"), identity.googleSubHash)
        assertEquals("user@example.com", identity.email)
        assertEquals("テストユーザー", identity.name)
    }

    @Test
    fun `異常系： email_verified=false の場合は GoogleIdTokenVerificationFailedException`() {
        val jwt =
            jwtBuilder()
                .subject("google-sub-123")
                .claim("email", "user@example.com")
                .claim("email_verified", false)
                .build()

        assertThrows(GoogleIdTokenVerificationFailedException::class.java) {
            verifier.extractIdentity(jwt)
        }
    }

    @Test
    fun `異常系： email_verified クレームが存在しない場合は GoogleIdTokenVerificationFailedException`() {
        val jwt =
            jwtBuilder()
                .subject("google-sub-123")
                .claim("email", "user@example.com")
                .build()

        assertThrows(GoogleIdTokenVerificationFailedException::class.java) {
            verifier.extractIdentity(jwt)
        }
    }

    @Test
    fun `異常系： sub クレームが存在しない場合は GoogleIdTokenVerificationFailedException`() {
        val jwt =
            jwtBuilder()
                .claim("email", "user@example.com")
                .claim("email_verified", true)
                .build()

        assertThrows(GoogleIdTokenVerificationFailedException::class.java) {
            verifier.extractIdentity(jwt)
        }
    }

    @Test
    fun `異常系： email クレームが存在しない場合は GoogleIdTokenVerificationFailedException`() {
        val jwt =
            jwtBuilder()
                .subject("google-sub-123")
                .claim("email_verified", true)
                .build()

        assertThrows(GoogleIdTokenVerificationFailedException::class.java) {
            verifier.extractIdentity(jwt)
        }
    }

    @Test
    fun `正常系： name クレームが存在しない場合は email が name として使われる`() {
        val jwt =
            jwtBuilder()
                .subject("google-sub-123")
                .claim("email", "user@example.com")
                .claim("email_verified", true)
                .build()

        val identity = verifier.extractIdentity(jwt)

        assertEquals("user@example.com", identity.name)
    }
}
