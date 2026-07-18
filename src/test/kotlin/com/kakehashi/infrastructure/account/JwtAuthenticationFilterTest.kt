package com.kakehashi.infrastructure.account

import com.kakehashi.domain.account.AccountId
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import java.time.Instant
import java.util.Date

/**
 * JwtAuthenticationFilter 単体テスト
 *
 * 設計書No：UC-A1
 * ADRNo：APP-ADR-0014
 *
 * ★★全体観点★★
 * 自前JWT検証カスタムフィルターが SecurityContextHolder への accountId セット・
 * 不正/期限切れJWTの401応答を正しく行うことを保証する。認可基盤（exec-plan 0007）は
 * このフィルターがセットする accountId を前提に設計されるため、認証基盤の根幹となる観点である。
 *
 * 《観　点》Authorization ヘッダーの有無によるフィルター通過判定の確認
 * 《テスト》正常系： 有効な自前JWTを付けたリクエストで SecurityContextHolder から accountId が取得できる
 * 《テスト》正常系： Authorization ヘッダーがないリクエストはそのまま後続フィルターへ通す
 *
 * 《観　点》不正・期限切れJWTを401で拒否しフィルターチェーンを止めることの確認
 * 《テスト》異常系： 署名鍵が異なるJWTは401を返しフィルターチェーンを止める
 * 《テスト》異常系： 期限切れJWTは401を返しフィルターチェーンを止める
 * 《テスト》異常系： Authorization ヘッダーが Bearer のみ（トークン部分が空文字）の場合は500ではなく401を返す
 */
class JwtAuthenticationFilterTest {
    private val secret = "test-only-jwt-secret-for-filter-test-min-32-bytes"
    private val jwtTokenIssuer = JwtTokenIssuerImpl(secret = secret, expirationSeconds = 3600)
    private val filter = JwtAuthenticationFilter(jwtTokenIssuer)

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `正常系： 有効な自前JWTを付けたリクエストで SecurityContextHolder から accountId が取得できる`() {
        val token = jwtTokenIssuer.issue(AccountId("AZ0001"))
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer $token")
        val response = MockHttpServletResponse()
        var chainCalled = false
        val chain = FilterChain { _, _ -> chainCalled = true }

        filter.doFilter(request, response, chain)

        assertEquals("AZ0001", SecurityContextHolder.getContext().authentication?.principal)
        assertTrue(chainCalled)
    }

    @Test
    fun `正常系： Authorization ヘッダーがないリクエストはそのまま後続フィルターへ通す`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        var chainCalled = false
        val chain = FilterChain { _, _ -> chainCalled = true }

        filter.doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
        assertTrue(chainCalled)
    }

    @Test
    fun `異常系： 署名鍵が異なるJWTは401を返しフィルターチェーンを止める`() {
        val otherSecretIssuer =
            JwtTokenIssuerImpl(secret = "different-jwt-secret-for-signature-mismatch-min-32-bytes", expirationSeconds = 3600)
        val tokenSignedWithOtherKey = otherSecretIssuer.issue(AccountId("AZ0001"))
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer $tokenSignedWithOtherKey")
        val response = MockHttpServletResponse()
        var chainCalled = false
        val chain = FilterChain { _, _ -> chainCalled = true }

        filter.doFilter(request, response, chain)

        assertEquals(401, response.status)
        assertFalse(chainCalled)
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `異常系： 期限切れJWTは401を返しフィルターチェーンを止める`() {
        val expiredToken =
            Jwts
                .builder()
                .subject("AZ0001")
                .claim("accountId", "AZ0001")
                .issuedAt(Date.from(Instant.now().minusSeconds(7200)))
                .expiration(Date.from(Instant.now().minusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8)))
                .compact()
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer $expiredToken")
        val response = MockHttpServletResponse()
        var chainCalled = false
        val chain = FilterChain { _, _ -> chainCalled = true }

        filter.doFilter(request, response, chain)

        assertEquals(401, response.status)
        assertFalse(chainCalled)
    }

    @Test
    fun `異常系： Authorization ヘッダーが Bearer のみ（トークン部分が空文字）の場合は500ではなく401を返す`() {
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer ")
        val response = MockHttpServletResponse()
        var chainCalled = false
        val chain = FilterChain { _, _ -> chainCalled = true }

        filter.doFilter(request, response, chain)

        assertEquals(401, response.status)
        assertFalse(chainCalled)
        assertNull(SecurityContextHolder.getContext().authentication)
    }
}
