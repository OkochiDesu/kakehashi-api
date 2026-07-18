package com.kakehashi.infrastructure.account

import jakarta.servlet.http.HttpServletResponse
import tools.jackson.databind.ObjectMapper

/**
 * 401 JSON レスポンスのシリアライズに使う ObjectMapper
 *
 * 根拠: Copilotレビュー指摘（PR #21）。手書きの文字列エスケープ（`\`・`"` のみ）では
 * `message` に改行・タブ等の制御文字が含まれる場合に不正な JSON になるため、
 * Jackson の [ObjectMapper] に JSON エンコーディングを委譲する。
 */
private val authErrorObjectMapper = ObjectMapper()

/**
 * 401 レスポンスの JSON ボディ（`{"code":...,"message":...}`）を表すデータ
 *
 * @property code エラーコード（例: `"JWT_VERIFICATION_FAILED"`, `"UNAUTHORIZED"`）
 * @property message エラーメッセージ
 */
private data class UnauthorizedErrorBody(
    val code: String,
    val message: String,
)

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
    response.writer.write(authErrorObjectMapper.writeValueAsString(UnauthorizedErrorBody(code = code, message = message)))
}
