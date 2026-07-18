package com.kakehashi.infrastructure.account

import jakarta.servlet.http.HttpServletResponse

/**
 * 認証エラー時の 401 JSON レスポンス（`{"code":...,"message":...}`）を書き込む共通処理
 *
 * 根拠: code-reviewer 指摘（PR #21）。[JwtAuthenticationFilter]（JWT検証失敗時）と
 * [RestAuthenticationEntryPoint]（未認証アクセス時）で 401 応答のフォーマットを
 * 統一するために共通化する。フォーマットは [com.kakehashi.presentation.GlobalExceptionHandler.ErrorResponse]
 * の `code` / `message` 構造に合わせている。
 *
 * @param response 書き込み対象のレスポンス
 * @param code エラーコード（例: `"JWT_VERIFICATION_FAILED"`, `"UNAUTHORIZED"`）
 * @param message エラーメッセージ
 */
internal fun writeUnauthorizedJson(
    response: HttpServletResponse,
    code: String,
    message: String,
) {
    response.status = HttpServletResponse.SC_UNAUTHORIZED
    response.contentType = "application/json;charset=UTF-8"
    val escapedMessage = message.replace("\\", "\\\\").replace("\"", "\\\"")
    response.writer.write("""{"code":"$code","message":"$escapedMessage"}""")
}
