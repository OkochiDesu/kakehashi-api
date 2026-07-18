package com.kakehashi.infrastructure.account

import com.kakehashi.domain.account.AccountId
import com.kakehashi.domain.account.JwtVerificationFailedException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * JwtTokenIssuerImpl 単体テスト
 *
 * 設計書No：UC-A1
 * ADRNo：APP-ADR-0014
 *
 * ★★全体観点★★
 * 自前 JWT の発行・検証（署名鍵管理方針は APP-ADR-0014 実装時決定を参照）が正しく往復すること、
 * および不正な入力（空文字列・改ざんされたトークン等）が [JwtVerificationFailedException]
 * （401 応答）に一貫して変換され、素通りして 500 Internal Server Error にならないことを保証する。
 *
 * 《観　点》正常な JWT の発行・検証の往復確認
 * 《テスト》正常系： issue() で発行した JWT を verify() すると同じ accountId が取得できる
 *
 * 《観　点》不正な形式のトークンが JwtVerificationFailedException に変換されることの確認（500化防止）
 * 《テスト》異常系： 空文字列のトークンは JwtVerificationFailedException（jjwt が IllegalArgumentException を
 *   投げるケースの捕捉漏れがないことを確認）
 * 《テスト》異常系： JWT の形式でない文字列は JwtVerificationFailedException
 */
class JwtTokenIssuerImplTest {
    private val secret = "test-only-jwt-secret-for-issuer-test-min-32-bytes"
    private val issuer = JwtTokenIssuerImpl(secret = secret, expirationSeconds = 3600)

    @Test
    fun `正常系： issue() で発行した JWT を verify() すると同じ accountId が取得できる`() {
        val token = issuer.issue(AccountId("AZ0001"))

        val accountId = issuer.verify(token)

        assertEquals("AZ0001", accountId.value)
    }

    @Test
    fun `異常系： 空文字列のトークンは JwtVerificationFailedException（jjwt が IllegalArgumentException を投げるケースの捕捉漏れがないことを確認）`() {
        assertThrows(JwtVerificationFailedException::class.java) { issuer.verify("") }
    }

    @Test
    fun `異常系： JWT の形式でない文字列は JwtVerificationFailedException`() {
        assertThrows(JwtVerificationFailedException::class.java) { issuer.verify("not-a-jwt-token") }
    }
}
