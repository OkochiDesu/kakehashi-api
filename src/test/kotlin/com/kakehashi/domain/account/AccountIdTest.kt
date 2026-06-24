package com.kakehashi.domain.account

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * AccountId 値オブジェクトの単体テスト
 *
 * 設計書No：-
 * ADRNo：APP-ADR-0001
 *
 * ★★全体観点★★
 * AccountId 値オブジェクトのフォーマットバリデーションを検証する。
 * フォーマット不正な ID が生成・流入すると DB 整合性が崩れるため、
 * 値オブジェクト生成時点で弾くことを保証する。
 *
 * ★★正常系★★
 * 《観　点》境界値（最小・最大）が受け入れられることの確認
 * 《テスト》AZ0001 形式は有効（最小シーケンス値）
 * 《テスト》AZ9999 形式は有効（最大シーケンス値）
 *
 * 《観　点》採番ロジック（fromSequence）がゼロ埋め4桁 ID を生成することの確認
 * 《テスト》fromSequence でゼロ埋め4桁の AccountId が生成される
 *
 * ★★異常系★★
 * 《観　点》プレフィックス・桁数・文字種のバリデーション境界の確認
 * 《テスト》プレフィックスなし（数字のみ）は不正フォーマット
 * 《テスト》数字が5桁は不正フォーマット（桁数オーバー）
 * 《テスト》数字が3桁は不正フォーマット（桁数不足）
 * 《テスト》プレフィックスが小文字は不正フォーマット（大文字必須）
 * 《テスト》空文字は不正フォーマット
 */
class AccountIdTest {
    @Test
    fun `正常系： AZ0001 形式は有効`() {
        val id = AccountId("AZ0001")
        assertEquals("AZ0001", id.value)
    }

    @Test
    fun `正常系： AZ9999 形式は有効`() {
        val id = AccountId("AZ9999")
        assertEquals("AZ9999", id.value)
    }

    @Test
    fun `正常系： fromSequence でゼロ埋め4桁の AccountId が生成される`() {
        val id = AccountId.fromSequence(42L)
        assertEquals("AZ0042", id.value)
    }

    @Test
    fun `異常系： プレフィックスなし（数字のみ）は不正フォーマット`() {
        assertThrows<IllegalArgumentException> {
            AccountId("0001")
        }
    }

    @Test
    fun `異常系： 数字が5桁は不正フォーマット`() {
        assertThrows<IllegalArgumentException> {
            AccountId("AZ00001")
        }
    }

    @Test
    fun `異常系： 数字が3桁は不正フォーマット`() {
        assertThrows<IllegalArgumentException> {
            AccountId("AZ001")
        }
    }

    @Test
    fun `異常系： プレフィックスが小文字は不正フォーマット`() {
        assertThrows<IllegalArgumentException> {
            AccountId("az0001")
        }
    }

    @Test
    fun `異常系： 空文字は不正フォーマット`() {
        assertThrows<IllegalArgumentException> {
            AccountId("")
        }
    }
}
