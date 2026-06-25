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
import org.junit.jupiter.api.assertThrows

/**
 * ListAccountsQuery 単体テスト
 *
 * 設計書No：UC-A5
 * ADRNo：APP-ADR-0006, APP-ADR-0008
 *
 * ★★全体観点★★
 * 検索 Query の 3 つの責務を独立して検証する。
 * ①権限による検索フィルタ強制（非 admin は active のみ）、
 * ②deactivated の個人情報マスキング（APP-ADR-0006）、
 * ③ページング計算の正確性。いずれも UI 直結のため誤動作がユーザー影響に直結する。
 *
 * 《観　点》AccountSummaryRow → Output 変換マッピングの基本動作確認
 * 《テスト》正常系： AccountMapper_searchAccounts の結果が変換されて返る
 * 《テスト》異常系： 不正な roleCode を渡すと IllegalArgumentException がスローされる
 *
 * 《観　点》権限による検索フィルタ強制の確認（非 admin は suspended/deactivated を見えないようにする）
 * 《テスト》正常系： isAdmin=false の場合 statuses が active に強制される
 * 《テスト》正常系： isAdmin=true かつ statuses 指定ありの場合は指定した statuses で検索する
 *
 * 《観　点》deactivated アカウントの個人情報が一覧 Query でも APP-ADR-0006 仕様通りにマスクされることの確認
 * 《テスト》正常系： deactivated アカウントの name は "***" にマスクされる
 *
 * 《観　点》端数切り上げのページ数計算が正確であることの確認
 * 《テスト》正常系： ページング計算が正しい（totalElements=21 size=20 → totalPages=2）
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

    @Nested
    inner class AbnormalCases {
        @Test
        fun `異常系： 不正な roleCode を渡すと IllegalArgumentException がスローされる`() {
            assertThrows<IllegalArgumentException> {
                query.execute(
                    ListAccountsQuery.Input(
                        name = null,
                        statuses = null,
                        roleCode = "invalid_role",
                        isAdmin = true,
                    ),
                )
            }
        }
    }
}
