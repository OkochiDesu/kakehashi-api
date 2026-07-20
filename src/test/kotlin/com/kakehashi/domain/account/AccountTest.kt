package com.kakehashi.domain.account

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.OffsetDateTime

/**
 * Account ドメインエンティティの単体テスト
 *
 * 設計書No：-
 * ADRNo：APP-ADR-0001, APP-ADR-0006, APP-ADR-0015
 *
 * ★★全体観点★★
 * Account 集約の各ビジネスメソッド（register / editName / suspend / unsuspend / assignRoles）が、
 * 状態遷移・バリデーション・version インクリメント・タイムスタンプ更新を
 * 集約単位で一貫して実行することを保証する。加えて、通常 class 化（APP-ADR-0015）に伴う
 * ID 基準の同一性判定・PII 安全な toString() が仕様通り機能することを保証する。
 *
 * 《観　点》register: PROVISIONAL → ACTIVE 遷移と集約状態の正確性確認
 * 《テスト》register - PROVISIONALからACTIVEへ遷移しversionが増加する
 * 《テスト》register - PROVISIONAL以外から呼ぶと例外をスローする
 *
 * 《観　点》editName: 表示名更新と変更追跡（version インクリメント）の確認
 * 《テスト》editName - 表示名を更新しversionが増加する
 * 《テスト》editName - 空文字列を渡すと例外をスローする
 *
 * 《観　点》suspend: ACTIVE → SUSPENDED 遷移と停止日時記録の確認
 * 《テスト》suspend - ACTIVEからSUSPENDEDへ遷移しsuspendedAtが設定される
 * 《テスト》suspend - ACTIVE以外から呼ぶと例外をスローする
 *
 * 《観　点》unsuspend: SUSPENDED → ACTIVE 遷移と停止日時クリアの確認
 * 《テスト》unsuspend - SUSPENDEDからACTIVEへ遷移しsuspendedAtがクリアされる
 * 《テスト》unsuspend - SUSPENDED以外から呼ぶと例外をスローする
 *
 * 《観　点》assignRoles: ロール変更に伴うversionインクリメントの確認
 * 《テスト》assignRoles - versionが増加しupdatedByが反映される
 *
 * 《観　点》ID基準のequals()/hashCode()（APP-ADR-0015）
 * 《テスト》正常系： accountIdが同一なら他フィールドが異なっても等価と判定される
 * 《テスト》正常系： accountIdが異なれば他フィールドが同じでも非等価と判定される
 *
 * 《観　点》PII安全なtoString()（APP-ADR-0015）
 * 《テスト》正常系： toString()はemail・googleSubHashを含まずaccountId・statusのみ含む
 */
class AccountTest {
    private fun buildAccount(
        status: AccountStatus = AccountStatus.PROVISIONAL,
        accountId: String = "AZ0001",
        name: String = "テストユーザー",
    ): Account =
        Account.reconstruct(
            accountId = AccountId(accountId),
            googleSubHash = "hash_$accountId",
            email = "user@example.com",
            name = name,
            status = status,
            suspendedAt = null,
            version = 0,
            createdBy = accountId,
            updatedBy = accountId,
            createdAt = OffsetDateTime.parse("2026-01-01T00:00:00+09:00"),
            updatedAt = OffsetDateTime.parse("2026-01-01T00:00:00+09:00"),
        )

    @Test
    fun `register - PROVISIONALからACTIVEへ遷移しversionが増加する`() {
        val account = buildAccount(AccountStatus.PROVISIONAL)
        val result = account.register("AZ0001")
        assertEquals(AccountStatus.ACTIVE, result.status)
        assertEquals(1, result.version)
    }

    @Test
    fun `register - PROVISIONAL以外から呼ぶと例外をスローする`() {
        val account = buildAccount(AccountStatus.ACTIVE)
        assertThrows<IllegalStateException> { account.register("AZ0001") }
    }

    @Test
    fun `editName - 表示名を更新しversionが増加する`() {
        val account = buildAccount(AccountStatus.ACTIVE)
        val result = account.editName("新しい名前", "AZ0001")
        assertEquals("新しい名前", result.name)
        assertEquals(1, result.version)
    }

    @Test
    fun `editName - 空文字列を渡すと例外をスローする`() {
        val account = buildAccount(AccountStatus.ACTIVE)
        assertThrows<IllegalArgumentException> { account.editName("   ", "AZ0001") }
    }

    @Test
    fun `suspend - ACTIVEからSUSPENDEDへ遷移しsuspendedAtが設定される`() {
        val account = buildAccount(AccountStatus.ACTIVE)
        val result = account.suspend("AZ0001")
        assertEquals(AccountStatus.SUSPENDED, result.status)
        assertNotNull(result.suspendedAt)
        assertEquals(1, result.version)
    }

    @Test
    fun `suspend - ACTIVE以外から呼ぶと例外をスローする`() {
        val account = buildAccount(AccountStatus.PROVISIONAL)
        assertThrows<IllegalStateException> { account.suspend("AZ0001") }
    }

    @Test
    fun `unsuspend - SUSPENDEDからACTIVEへ遷移しsuspendedAtがクリアされる`() {
        val account = buildAccount(AccountStatus.SUSPENDED)
        val result = account.unsuspend("AZ0001")
        assertEquals(AccountStatus.ACTIVE, result.status)
        assertNull(result.suspendedAt)
        assertEquals(1, result.version)
    }

    @Test
    fun `unsuspend - SUSPENDED以外から呼ぶと例外をスローする`() {
        val account = buildAccount(AccountStatus.ACTIVE)
        assertThrows<IllegalStateException> { account.unsuspend("AZ0001") }
    }

    @Test
    fun `assignRoles - versionが増加しupdatedByが反映される`() {
        val account = buildAccount(status = AccountStatus.ACTIVE)
        val result = account.assignRoles("OPERATOR")
        assertEquals(1, result.version)
        assertEquals("OPERATOR", result.updatedBy)
    }

    @Test
    fun `正常系： accountIdが同一なら他フィールドが異なっても等価と判定される`() {
        val account1 = buildAccount(accountId = "AZ0001", name = "名前A")
        val account2 = buildAccount(accountId = "AZ0001", name = "名前B")
        assertEquals(account1, account2)
        assertEquals(account1.hashCode(), account2.hashCode())
    }

    @Test
    fun `正常系： accountIdが異なれば他フィールドが同じでも非等価と判定される`() {
        val account1 = buildAccount(accountId = "AZ0001")
        val account2 = buildAccount(accountId = "AZ0002")
        assertNotEquals(account1, account2)
    }

    @Test
    fun `正常系： toString()はemail・googleSubHashを含まずaccountId・statusのみ含む`() {
        val account = buildAccount(accountId = "AZ0001", status = AccountStatus.ACTIVE)
        val result = account.toString()
        assertTrue(result.contains("AZ0001"))
        assertTrue(result.contains("ACTIVE"))
        assertFalse(result.contains("user@example.com"))
        assertFalse(result.contains("hash_az0001"))
    }
}
