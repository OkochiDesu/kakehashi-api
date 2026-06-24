package com.kakehashi.domain.account

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.OffsetDateTime

/**
 * Account ドメインエンティティの単体テスト
 *
 * 設計書No：-
 * ADRNo：APP-ADR-0001, APP-ADR-0006
 *
 * ★★全体観点★★
 * Account 集約の各ビジネスメソッド（register / editName / suspend / unsuspend）が、
 * 状態遷移・バリデーション・version インクリメント・タイムスタンプ更新を
 * 集約単位で一貫して実行することを保証する。
 *
 * ★★正常系★★
 * 《観　点》register: PROVISIONAL → ACTIVE 遷移と集約状態の正確性確認
 * 《テスト》register - PROVISIONALからACTIVEへ遷移しversionが増加する
 *
 * 《観　点》editName: 表示名更新と変更追跡（version インクリメント）の確認
 * 《テスト》editName - 表示名を更新しversionが増加する
 *
 * 《観　点》suspend: ACTIVE → SUSPENDED 遷移と停止日時記録の確認
 * 《テスト》suspend - ACTIVEからSUSPENDEDへ遷移しsuspendedAtが設定される
 *
 * 《観　点》unsuspend: SUSPENDED → ACTIVE 遷移と停止日時クリアの確認
 * 《テスト》unsuspend - SUSPENDEDからACTIVEへ遷移しsuspendedAtがクリアされる
 *
 * ★★異常系★★
 * 《観　点》register: PROVISIONAL 以外からの登録を禁止（重複登録・誤操作の防止）
 * 《テスト》register - PROVISIONAL以外から呼ぶと例外をスローする
 *
 * 《観　点》editName: 空文字列による表示名の空欄化を防止
 * 《テスト》editName - 空文字列を渡すと例外をスローする
 *
 * 《観　点》suspend: ACTIVE 以外からの停止操作を禁止（不正な状態変更の防止）
 * 《テスト》suspend - ACTIVE以外から呼ぶと例外をスローする
 *
 * 《観　点》unsuspend: SUSPENDED 以外からの解除操作を禁止（不正な状態変更の防止）
 * 《テスト》unsuspend - SUSPENDED以外から呼ぶと例外をスローする
 */
class AccountTest {
    private fun buildAccount(status: AccountStatus = AccountStatus.PROVISIONAL): Account =
        Account(
            accountId = AccountId("AZ0001"),
            googleSubHash = "hash_az0001",
            email = "user@example.com",
            name = "テストユーザー",
            status = status,
            suspendedAt = null,
            version = 0,
            createdBy = "AZ0001",
            updatedBy = "AZ0001",
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
}
