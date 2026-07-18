package com.kakehashi.infrastructure.account

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.InsufficientAuthenticationException

/**
 * RestAuthenticationEntryPoint 単体テスト
 *
 * 設計書No：UC-A1
 * ADRNo：APP-ADR-0014
 *
 * ★★全体観点★★
 * `httpBasic()` / `formLogin()` を設定しない [com.kakehashi.config.SecurityConfig] 構成では
 * Spring Security のデフォルト `AuthenticationEntryPoint` が `Http403ForbiddenEntryPoint` に
 * フォールバックし、未認証アクセスが 403 Forbidden になってしまう問題（code-reviewer 指摘、PR #21）に
 * 対する回避策が機能することを保証する。`docs/design/api/account-role.md` が規定する
 * 「401 Unauthorized: 未認証」を満たす根幹のテストである。
 *
 * 《観　点》未認証アクセスに対して常に401を返すことの確認
 * 《テスト》正常系： commence() 呼び出しで401とJSONレスポンス（code=UNAUTHORIZED）を返す
 * 《テスト》正常系： AuthenticationException の種類によらず401を返す（InsufficientAuthenticationException）
 */
class RestAuthenticationEntryPointTest {
    private val entryPoint = RestAuthenticationEntryPoint()

    @Test
    fun `正常系： commence() 呼び出しで401とJSONレスポンス（code=UNAUTHORIZED）を返す`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        entryPoint.commence(request, response, BadCredentialsException("test"))

        assertEquals(401, response.status)
        assertTrue(response.contentType?.startsWith("application/json") ?: false)
        assertTrue(response.contentAsString.contains("\"code\":\"UNAUTHORIZED\""))
    }

    @Test
    fun `正常系： AuthenticationException の種類によらず401を返す（InsufficientAuthenticationException）`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        entryPoint.commence(request, response, InsufficientAuthenticationException("認証が必要です"))

        assertEquals(401, response.status)
    }
}
