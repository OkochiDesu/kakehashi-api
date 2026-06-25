package com.kakehashi.domain.account

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * AccountStatus の単体テスト
 *
 * 設計書No：-
 * ADRNo：APP-ADR-0006
 *
 * ★★全体観点★★
 * APP-ADR-0006 で定義した 4 値ステータスの遷移グラフ・ログイン可否・検索対象可否の
 * 3 つの判定ロジックが仕様通りに動作することを保証する。
 * 許容・禁止の全パターンを網羅し、不正遷移によるデータ不整合を防ぐ。
 *
 * 《観　点》canTransitionTo: 許容される遷移・禁止される遷移の確認
 * 《テスト》正常系： PROVISIONAL から ACTIVE へ遷移できる
 * 《テスト》正常系： ACTIVE から SUSPENDED へ遷移できる
 * 《テスト》正常系： SUSPENDED から ACTIVE へ遷移できる
 * 《テスト》正常系： SUSPENDED から DEACTIVATED へ遷移できる（日次バッチ）
 * 《テスト》異常系： PROVISIONAL から SUSPENDED へは遷移できない
 * 《テスト》異常系： PROVISIONAL から DEACTIVATED へは遷移できない
 * 《テスト》異常系： ACTIVE から PROVISIONAL へは遷移できない
 * 《テスト》異常系： ACTIVE から DEACTIVATED へは遷移できない
 * 《テスト》異常系： SUSPENDED から PROVISIONAL へは遷移できない
 * 《テスト》異常系： DEACTIVATED からはいかなるステータスへも遷移できない
 *
 * 《観　点》canLogin / isSearchable: ACTIVE のみがログイン・検索対象の境界値確認
 * 《テスト》正常系： ACTIVE のみ canLogin が true を返す
 * 《テスト》正常系： ACTIVE のみ isSearchable が true を返す
 * 《テスト》異常系： ACTIVE 以外は canLogin が false を返す
 * 《テスト》異常系： ACTIVE 以外は isSearchable が false を返す
 *
 * 《観　点》fromDbValue: DB 文字列から enum への変換の確認
 * 《テスト》正常系： active から ACTIVE に変換できる
 * 《テスト》正常系： suspended から SUSPENDED に変換できる
 * 《テスト》異常系： 未知の値は IllegalArgumentException を投げる
 */
class AccountStatusTest {
    @Nested
    inner class CanTransitionTo {
        @Test
        fun `正常系： PROVISIONAL から ACTIVE へ遷移できる`() {
            assertTrue(AccountStatus.PROVISIONAL.canTransitionTo(AccountStatus.ACTIVE))
        }

        @Test
        fun `異常系： PROVISIONAL から SUSPENDED へは遷移できない`() {
            assertFalse(AccountStatus.PROVISIONAL.canTransitionTo(AccountStatus.SUSPENDED))
        }

        @Test
        fun `異常系： PROVISIONAL から DEACTIVATED へは遷移できない`() {
            assertFalse(AccountStatus.PROVISIONAL.canTransitionTo(AccountStatus.DEACTIVATED))
        }

        @Test
        fun `正常系： ACTIVE から SUSPENDED へ遷移できる`() {
            assertTrue(AccountStatus.ACTIVE.canTransitionTo(AccountStatus.SUSPENDED))
        }

        @Test
        fun `異常系： ACTIVE から PROVISIONAL へは遷移できない`() {
            assertFalse(AccountStatus.ACTIVE.canTransitionTo(AccountStatus.PROVISIONAL))
        }

        @Test
        fun `異常系： ACTIVE から DEACTIVATED へは遷移できない`() {
            assertFalse(AccountStatus.ACTIVE.canTransitionTo(AccountStatus.DEACTIVATED))
        }

        @Test
        fun `正常系： SUSPENDED から ACTIVE へ遷移できる`() {
            assertTrue(AccountStatus.SUSPENDED.canTransitionTo(AccountStatus.ACTIVE))
        }

        @Test
        fun `正常系： SUSPENDED から DEACTIVATED へ遷移できる（日次バッチ）`() {
            assertTrue(AccountStatus.SUSPENDED.canTransitionTo(AccountStatus.DEACTIVATED))
        }

        @Test
        fun `異常系： SUSPENDED から PROVISIONAL へは遷移できない`() {
            assertFalse(AccountStatus.SUSPENDED.canTransitionTo(AccountStatus.PROVISIONAL))
        }

        @Test
        fun `異常系： DEACTIVATED からはいかなるステータスへも遷移できない`() {
            AccountStatus.entries.forEach { next ->
                assertFalse(AccountStatus.DEACTIVATED.canTransitionTo(next))
            }
        }
    }

    @Nested
    inner class CanLogin {
        @Test
        fun `正常系： ACTIVE のみ canLogin が true を返す`() {
            assertTrue(AccountStatus.ACTIVE.canLogin())
        }

        @Test
        fun `異常系： ACTIVE 以外は canLogin が false を返す`() {
            listOf(AccountStatus.PROVISIONAL, AccountStatus.SUSPENDED, AccountStatus.DEACTIVATED).forEach {
                assertFalse(it.canLogin())
            }
        }
    }

    @Nested
    inner class IsSearchable {
        @Test
        fun `正常系： ACTIVE のみ isSearchable が true を返す`() {
            assertTrue(AccountStatus.ACTIVE.isSearchable())
        }

        @Test
        fun `異常系： ACTIVE 以外は isSearchable が false を返す`() {
            listOf(AccountStatus.PROVISIONAL, AccountStatus.SUSPENDED, AccountStatus.DEACTIVATED).forEach {
                assertFalse(it.isSearchable())
            }
        }
    }

    @Nested
    inner class FromDbValue {
        @Test
        fun `正常系： active から ACTIVE に変換できる`() {
            assertEquals(AccountStatus.ACTIVE, AccountStatus.fromDbValue("active"))
        }

        @Test
        fun `正常系： suspended から SUSPENDED に変換できる`() {
            assertEquals(AccountStatus.SUSPENDED, AccountStatus.fromDbValue("suspended"))
        }

        @Test
        fun `異常系： 未知の値は IllegalArgumentException を投げる`() {
            assertThrows<IllegalArgumentException> {
                AccountStatus.fromDbValue("unknown_status")
            }
        }
    }
}
