package com.kakehashi.infrastructure.account

import com.kakehashi.domain.account.JwtTokenIssuer
import com.kakehashi.domain.account.JwtVerificationFailedException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpMethod
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
 * - [GOOGLE_CALLBACK_PATH]（[com.kakehashi.config.SecurityConfig] の permitAll 対象）は
 *   [shouldNotFilter] でフィルター自体をスキップする。クライアントが古い/不正な
 *   `Authorization` ヘッダーを付与したまま再ログインしようとした場合に、本フィルターが
 *   先に 401 を返してログイン用エンドポイントに到達できなくなるのを防ぐため（Copilotレビュー指摘、PR #21）
 * - 上記以外で Authorization ヘッダーが存在しない、または `Bearer ` 形式（大文字小文字は区別しない）
 *   でない場合はそのまま後続へ通す（エンドポイント側の認可設定で判定する）
 * - スキームとトークンの間に余分な空白がある場合はトリムしてから検証する
 *   （HTTP の auth-scheme は大文字小文字を区別しないため `bearer` 表記や空白混在も許容する）
 * - トークンが存在するが検証に失敗した場合（署名不正・有効期限切れ・トリム後に空文字等）は
 *   401 を返しフィルターチェーンを止める
 *
 * @property jwtTokenIssuer 自前 JWT の検証ポート（[JwtTokenIssuer.verify]）
 */
class JwtAuthenticationFilter(
    private val jwtTokenIssuer: JwtTokenIssuer,
) : OncePerRequestFilter() {
    /**
     * [GOOGLE_CALLBACK_PATH] への POST リクエストかどうかを判定し、本フィルターの適用対象から除外する。
     *
     * @param request 判定対象のリクエスト
     * @return permitAll 対象（POST [GOOGLE_CALLBACK_PATH]）の場合 true
     */
    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.method == HttpMethod.POST.name() && request.requestURI == GOOGLE_CALLBACK_PATH

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader(AUTHORIZATION_HEADER)
        if (header == null || !header.startsWith(BEARER_PREFIX, ignoreCase = true)) {
            filterChain.doFilter(request, response)
            return
        }

        val token = header.substring(BEARER_PREFIX.length).trim()
        if (token.isEmpty()) {
            writeUnauthorized(response, "Authorization ヘッダーにトークンが含まれていません")
            return
        }

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

        /**
         * Google SSO コールバック（UC-A1）の permitAll 対象パス。
         * [com.kakehashi.config.SecurityConfig] の matcher と値を一致させること（drift 防止）。
         */
        internal const val GOOGLE_CALLBACK_PATH = "/api/auth/google/callback"
    }
}
