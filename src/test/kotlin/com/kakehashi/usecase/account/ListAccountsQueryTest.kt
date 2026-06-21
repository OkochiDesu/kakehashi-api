package com.kakehashi.usecase.account

import com.kakehashi.domain.account.AccountStatus
import com.kakehashi.infrastructure.account.AccountMapper
import com.kakehashi.infrastructure.account.AccountSummaryRow
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * ListAccountsQuery 単体テスト
 *
 * 設計書No：UC-A5
 * ADRNo：APP-ADR-0006, APP-ADR-0008
 */
class ListAccountsQueryTest {
    private val accountMapper = mockk<AccountMapper>()
    private val query = ListAccountsQuery(accountMapper)

    private val activeRow =
        AccountSummaryRow(
            accountId = "AZ0001",
            name = "アクティブユーザー",
            status = "active",
        )

    private val deactivatedRow =
        AccountSummaryRow(
            accountId = "AZ0002",
            name = "退会ユーザー本名",
            status = "deactivated",
        )

    @Nested
    inner class NormalCases {
        @Test
        fun `正常系： AccountMapper_searchAccounts の結果が変換されて返る`() {
            every { accountMapper.searchAccounts(any(), any(), any(), any(), any()) } returns listOf(activeRow)
            every { accountMapper.countAccounts(any(), any(), any()) } returns 1L

            val input =
                ListAccountsQuery.Input(
                    name = null,
                    statuses = null,
                    roleCode = null,
                    isAdmin = true,
                )
            val output = query.execute(input)

            assertEquals(1, output.content.size)
            assertEquals("AZ0001", output.content.first().accountId)
            assertEquals("アクティブユーザー", output.content.first().name)
            assertEquals(1L, output.totalElements)
        }

        @Test
        fun `正常系： isAdmin=false の場合 statuses が active に強制される`() {
            val statusesSlot = slot<List<String>>()
            every { accountMapper.searchAccounts(any(), capture(statusesSlot), any(), any(), any()) } returns emptyList()
            every { accountMapper.countAccounts(any(), any(), any()) } returns 0L

            val input =
                ListAccountsQuery.Input(
                    name = null,
                    statuses = listOf("suspended"), // 非管理者が suspended を指定しても無視される
                    roleCode = null,
                    isAdmin = false,
                )
            query.execute(input)

            // 非管理者の statuses は active に強制されることを検証
            assertEquals(listOf(AccountStatus.ACTIVE.toDbValue()), statusesSlot.captured)
        }

        @Test
        fun `正常系： isAdmin=true かつ statuses 指定ありの場合は指定した statuses で検索する`() {
            val statusesSlot = slot<List<String>>()
            every { accountMapper.searchAccounts(any(), capture(statusesSlot), any(), any(), any()) } returns emptyList()
            every { accountMapper.countAccounts(any(), any(), any()) } returns 0L

            val input =
                ListAccountsQuery.Input(
                    name = null,
                    statuses = listOf("suspended"),
                    roleCode = null,
                    isAdmin = true,
                )
            query.execute(input)

            assertEquals(listOf("suspended"), statusesSlot.captured)
        }

        @Test
        fun `正常系： deactivated アカウントの name は "***" にマスクされる`() {
            every { accountMapper.searchAccounts(any(), any(), any(), any(), any()) } returns listOf(deactivatedRow)
            every { accountMapper.countAccounts(any(), any(), any()) } returns 1L

            val input =
                ListAccountsQuery.Input(
                    name = null,
                    statuses = listOf("deactivated"),
                    roleCode = null,
                    isAdmin = true,
                )
            val output = query.execute(input)

            assertEquals("***", output.content.first().name)
            assertEquals("AZ0002", output.content.first().accountId)
        }

        @Test
        fun `正常系： ページング計算が正しい（totalElements=21 size=20 → totalPages=2）`() {
            every { accountMapper.searchAccounts(any(), any(), any(), any(), any()) } returns emptyList()
            every { accountMapper.countAccounts(any(), any(), any()) } returns 21L

            val input =
                ListAccountsQuery.Input(
                    name = null,
                    statuses = null,
                    roleCode = null,
                    page = 0,
                    size = 20,
                    isAdmin = true,
                )
            val output = query.execute(input)

            assertEquals(2, output.totalPages)
        }
    }
}
