package com.kakehashi.infrastructure.account

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint

/**
 * 未認証アクセス時に 401 Unauthorized を返す AuthenticationEntryPoint（APP-ADR-0014）
 *
 * 根拠: docs/design/api/account-role.md（保護エンドポイントの4xxレスポンス: 401 Unauthorized＝未認証）
 *
 * [com.kakehashi.config.SecurityConfig] が `httpBasic()` / `formLogin()` 等の認証方式を
 * 明示的に設定しない構成では、Spring Security のデフォルト `AuthenticationEntryPoint` が
 * `Http403ForbiddenEntryPoint` にフォールバックし、未認証アクセスが 401 ではなく
 * 403 Forbidden になってしまう（code-reviewer 指摘、PR #21）。本クラスを
 * `HttpSecurity.exceptionHandling { it.authenticationEntryPoint(...) }` に明示的に登録することで
 * 常に 401 を返すようにする。
 *
 * [JwtAuthenticationFilter.writeUnauthorized] とレスポンス形式（JSON: code/message）を統一するため
 * [writeUnauthorizedJson] を共用する。
 *
 * `@Component` を付与せず [com.kakehashi.config.SecurityConfig] から明示的にインスタンス化する
 * （`@WebMvcTest` の型ベーススキャンで意図せず取り込まれるおそれがあるため。[JwtAuthenticationFilter] と同じ方針）。
 */
class RestAuthenticationEntryPoint : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        writeUnauthorizedJson(response, "UNAUTHORIZED", "認証が必要です")
    }
}
