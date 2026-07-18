package com.kakehashi.config

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * AllowedDomainsParser 単体テスト
 *
 * 設計書No：-
 * ADRNo：APP-ADR-0014
 *
 * ★★全体観点★★
 * `app.auth.google.allowed-domains` のパース仕様（トリム・小文字化・空要素除外）が
 * `AccountUseCaseConfig`・`AuthStartupValidator` 双方で一貫することを保証する。
 * カンマのみの値等が「非空だが有効ドメイン0件」に正規化されることを確認する。
 *
 * 《観　点》カンマ区切り文字列が正しく正規化されることの確認
 * 《テスト》正常系： 単一ドメインはそのまま1件の集合になる
 * 《テスト》正常系： 複数ドメインはカンマ区切りで集合になる
 * 《テスト》正常系： 前後の空白はトリムされる
 * 《テスト》正常系： 大文字小文字は小文字に正規化される
 * 《テスト》正常系： 重複ドメインは1件に集約される
 *
 * 《観　点》空・不正な値が空集合として正規化されることの確認
 * 《テスト》正常系： 空文字列は空集合になる
 * 《テスト》正常系： カンマのみの値は空集合になる（isNotBlank()は通過するが有効ドメインなし）
 * 《テスト》正常系： 空白のみの値は空集合になる
 */
class AllowedDomainsParserTest {
    @Test
    fun `正常系： 単一ドメインはそのまま1件の集合になる`() {
        val result = AllowedDomainsParser.parse("example.com")

        assertEquals(setOf("example.com"), result)
    }

    @Test
    fun `正常系： 複数ドメインはカンマ区切りで集合になる`() {
        val result = AllowedDomainsParser.parse("example.com,example.co.jp")

        assertEquals(setOf("example.com", "example.co.jp"), result)
    }

    @Test
    fun `正常系： 前後の空白はトリムされる`() {
        val result = AllowedDomainsParser.parse(" example.com , example.co.jp ")

        assertEquals(setOf("example.com", "example.co.jp"), result)
    }

    @Test
    fun `正常系： 大文字小文字は小文字に正規化される`() {
        val result = AllowedDomainsParser.parse("Example.COM")

        assertEquals(setOf("example.com"), result)
    }

    @Test
    fun `正常系： 重複ドメインは1件に集約される`() {
        val result = AllowedDomainsParser.parse("example.com,example.com")

        assertEquals(setOf("example.com"), result)
    }

    @Test
    fun `正常系： 空文字列は空集合になる`() {
        val result = AllowedDomainsParser.parse("")

        assertEquals(emptySet(), result)
    }

    @Test
    fun `正常系： カンマのみの値は空集合になる（isNotBlank()は通過するが有効ドメインなし）`() {
        val result = AllowedDomainsParser.parse(",,")

        assertEquals(emptySet(), result)
    }

    @Test
    fun `正常系： 空白のみの値は空集合になる`() {
        val result = AllowedDomainsParser.parse("   ")

        assertEquals(emptySet(), result)
    }
}
