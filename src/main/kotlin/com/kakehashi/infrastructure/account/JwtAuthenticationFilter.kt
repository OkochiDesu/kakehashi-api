package com.kakehashi.infrastructure.account

import com.kakehashi.domain.account.JwtTokenIssuer
import com.kakehashi.domain.account.JwtVerificationFailedException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 自前 JWT 検証カスタムフィルター（APP-ADR-0014）
 *
 * 根拠: docs/design/api/account-role.md（認証・認可節）
 *
 * `Authorization: Bearer <自前JWT>` ヘッダーを検証し、成功時は [SecurityContextHolder] に
 * accountId を principal とする認証情報をセットする。ロールベースの認可判定
 * （`@PreAuthorize` 等）は exec-plan 0007 のスコープであり本フィルターの責務ではない。
 *
 * - Authorization ヘッダーが存在しない、または `Bearer ` 形式でない場合はそのまま後続へ通す
 *   （エンドポイント側の認可設定・`/api/auth/google/callback` の permitAll 等で判定する）
 * - トークンが存在するが検証に失敗した場合（署名不正・有効期限切れ等）は 401 を返しフィルターチェーンを止める
 *
 * @property jwtTokenIssuer 自前 JWT の検証ポート（[JwtTokenIssuer.verify]）
 */
class JwtAuthenticationFilter(
    private val jwtTokenIssuer: JwtTokenIssuer,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader(AUTHORIZATION_HEADER)
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response)
            return
        }

        val token = header.removePrefix(BEARER_PREFIX)
        val accountId =
            try {
                jwtTokenIssuer.verify(token)
            } catch (e: JwtVerificationFailedException) {
                writeUnauthorized(response, e.message ?: "JWTの検証に失敗しました")
                return
            }

        val authentication = UsernamePasswordAuthenticationToken(accountId.value, null, emptyList())
        SecurityContextHolder.getContext().authentication = authentication

        filterChain.doFilter(request, response)
    }

    /**
     * JWT検証失敗時の401 JSONレスポンスを書き込む（[writeUnauthorizedJson]に委譲）。
     *
     * @param response 書き込み対象のレスポンス
     * @param message エラーメッセージ
     */
    private fun writeUnauthorized(
        response: HttpServletResponse,
        message: String,
    ) {
        writeUnauthorizedJson(response, "JWT_VERIFICATION_FAILED", message)
    }

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }
}
