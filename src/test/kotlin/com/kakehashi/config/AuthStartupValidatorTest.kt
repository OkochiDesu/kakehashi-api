package com.kakehashi.config

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * AuthStartupValidator 単体テスト
 *
 * 設計書No：UC-A1
 * ADRNo：APP-ADR-0014
 *
 * ★★全体観点★★
 * 本番相当プロファイルでの起動時に、JWT署名鍵のデフォルト値サイレントフォールバック・
 * Google許可ドメイン未設定を検知して fail-fast させることを保証する。この検証を怠ると
 * 公知の開発用鍵でJWTが偽造可能になる、または社外の任意のGoogleアカウントでログインが
 * 成立してしまうため、認証基盤のセキュリティ根幹を担うテストである。
 *
 * 《観　点》JWT署名鍵の検証
 * 《テスト》正常系： デフォルト値と異なり32byte以上の鍵・非空の許可ドメインなら例外をスローしない
 * 《テスト》異常系： JWT署名鍵が開発用デフォルト値のままの場合は IllegalStateException
 * 《テスト》異常系： JWT署名鍵が32byte(256bit)未満の場合は IllegalStateException
 *
 * 《観　点》Google許可ドメインの検証
 * 《テスト》異常系： Google許可ドメインが空文字の場合は IllegalStateException
 * 《テスト》異常系： Google許可ドメインが空白のみの場合は IllegalStateException
 * 《テスト》異常系： Google許可ドメインがカンマのみ（パース後に空集合）の場合は IllegalStateException
 */
class AuthStartupValidatorTest {
    private val validSecret = "production-only-jwt-secret-value-min-32-bytes-long"
    private val validAllowedDomains = "example.com"

    @Test
    fun `正常系： デフォルト値と異なり32byte以上の鍵・非空の許可ドメインなら例外をスローしない`() {
        val validator =
            AuthStartupValidator(
                jwtSecret = validSecret,
                allowedDomains = validAllowedDomains,
            )

        assertDoesNotThrow { validator.validate() }
    }

    @Test
    fun `異常系： JWT署名鍵が開発用デフォルト値のままの場合は IllegalStateException`() {
        val validator =
            AuthStartupValidator(
                jwtSecret = "dev-only-jwt-secret-please-override-in-production-min-32bytes",
                allowedDomains = validAllowedDomains,
            )

        assertThrows(IllegalStateException::class.java) { validator.validate() }
    }

    @Test
    fun `異常系： JWT署名鍵が32byte(256bit)未満の場合は IllegalStateException`() {
        val validator =
            AuthStartupValidator(
                jwtSecret = "short-secret-under-32-bytes",
                allowedDomains = validAllowedDomains,
            )

        assertThrows(IllegalStateException::class.java) { validator.validate() }
    }

    @Test
    fun `異常系： Google許可ドメインが空文字の場合は IllegalStateException`() {
        val validator =
            AuthStartupValidator(
                jwtSecret = validSecret,
                allowedDomains = "",
            )

        assertThrows(IllegalStateException::class.java) { validator.validate() }
    }

    @Test
    fun `異常系： Google許可ドメインが空白のみの場合は IllegalStateException`() {
        val validator =
            AuthStartupValidator(
                jwtSecret = validSecret,
                allowedDomains = "   ",
            )

        assertThrows(IllegalStateException::class.java) { validator.validate() }
    }

    @Test
    fun `異常系： Google許可ドメインがカンマのみ（パース後に空集合）の場合は IllegalStateException`() {
        // isNotBlank() は通過するが、AllowedDomainsParser.parse() では空集合になるケース
        val validator =
            AuthStartupValidator(
                jwtSecret = validSecret,
                allowedDomains = ",,",
            )

        assertThrows(IllegalStateException::class.java) { validator.validate() }
    }
}
