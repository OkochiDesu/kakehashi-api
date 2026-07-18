package com.kakehashi.infrastructure.account

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletResponse
import tools.jackson.databind.ObjectMapper

/**
 * AuthErrorResponseWriter（writeUnauthorizedJson）単体テスト
 *
 * 設計書No：UC-A1
 * ADRNo：APP-ADR-0014
 *
 * ★★全体観点★★
 * 401 JSONレスポンスの message フィールドに制御文字（改行・タブ等）や引用符・バックスラッシュが
 * 含まれていても、常に valid な JSON として出力されることを保証する。手書きエスケープの漏れは
 * レスポンスボディの構文崩壊・後続処理でのパース例外に直結するため重要な観点である（Copilotレビュー指摘、PR #21）。
 *
 * 《観　点》通常メッセージのJSON出力
 * 《テスト》正常系： 制御文字を含まないメッセージは code・message を含む有効なJSONとして出力される
 *
 * 《観　点》エスケープが必要な文字を含むメッセージのJSON出力
 * 《テスト》正常系： バックスラッシュとダブルクォートを含むメッセージでも有効なJSONとして出力される
 * 《テスト》正常系： 改行・タブ・復帰等の制御文字を含むメッセージでも有効なJSONとして出力される
 */
class AuthErrorResponseWriterTest {
    private val objectMapper = ObjectMapper()

    @Test
    fun `正常系： 制御文字を含まないメッセージは code・message を含む有効なJSONとして出力される`() {
        val response = MockHttpServletResponse()

        writeUnauthorizedJson(response, "UNAUTHORIZED", "認証が必要です")

        assertEquals(401, response.status)
        val body = objectMapper.readTree(response.contentAsString)
        assertEquals("UNAUTHORIZED", body.get("code").asString())
        assertEquals("認証が必要です", body.get("message").asString())
    }

    @Test
    fun `正常系： バックスラッシュとダブルクォートを含むメッセージでも有効なJSONとして出力される`() {
        val response = MockHttpServletResponse()
        val message = """不正な"トークン"です\パス区切り"""

        writeUnauthorizedJson(response, "JWT_VERIFICATION_FAILED", message)

        val body = objectMapper.readTree(response.contentAsString)
        assertEquals(message, body.get("message").asString())
    }

    @Test
    fun `正常系： 改行・タブ・復帰等の制御文字を含むメッセージでも有効なJSONとして出力される`() {
        val response = MockHttpServletResponse()
        val message = "1行目\n2行目\tタブ区切り\r復帰文字"

        writeUnauthorizedJson(response, "JWT_VERIFICATION_FAILED", message)

        val body = objectMapper.readTree(response.contentAsString)
        assertEquals(message, body.get("message").asString())
    }
}
