package com.kakehashi.usecase.account

import com.kakehashi.domain.account.AccountId
import com.kakehashi.infrastructure.account.AccountDetailRow
import com.kakehashi.infrastructure.account.AccountMapper
import com.kakehashi.infrastructure.account.RoleRow
import com.kakehashi.usecase.account.exception.AccountNotFoundException
import com.kakehashi.usecase.account.exception.ForbiddenOperationException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * GetAccountQuery 単体テスト
 *
 * 設計書No：-
 * ADRNo：APP-ADR-0006, APP-ADR-0008
 *
 * ★★全体観点★★
 * CQRS Query 側のアクセス制御（admin vs 本人のみ）と個人情報マスキング（APP-ADR-0006）が
 * 正しく機能することを保証する。UI 直結データのため誤表示はセキュリティ問題に直結する。
 *
 * 《観　点》admin・非 admin のアクセス制御と個人情報マスキングの確認
 * 《テスト》正常系： admin が他人のアカウントを取得できる
 * 《テスト》正常系： 非 admin が自分のアカウントを取得できる
 * 《テスト》正常系： deactivated アカウントの name と email が "***" にマスクされる
 * 《テスト》異常系： 非 admin が他人のアカウントにアクセスすると ForbiddenOperationException
 *
 * 《観　点》RoleRow → RoleCode の変換マッピングが正確であることの確認
 * 《テスト》正常系： ロール情報が正しく変換されて返る
 *
 * 《観　点》不在リソースへの取得が安全に失敗することの確認
 * 《テスト》異常系： アカウントが存在しない場合は AccountNotFoundException
 */
class GetAccountQueryTest {
    private val accountMapper = mockk<AccountMapper>()
    private val query = GetAccountQuery(accountMapper)

    private val targetAccountId = AccountId("AZ0001")

    private fun buildDetailRow(
        accountId: String = "AZ0001",
        name: String = "テストユーザー",
        email: String = "user@example.com",
        status: String = "active",
        roles: List<RoleRow> = emptyList(),
    ) = AccountDetailRow(
        accountId = accountId,
        name = name,
        email = email,
        status = status,
        suspendedAt = null,
        version = 0,
        createdAt = "2026-01-01T00:00:00+09:00",
        updatedAt = "2026-01-01T00:00:00+09:00",
        updatedBy = accountId,
        roles = roles,
    )

    @Nested
    inner class NormalCases {
        @Test
        fun `正常系： admin が他人のアカウントを取得できる`() {
            val row = buildDetailRow()
            every { accountMapper.findAccountDetailById("AZ0001") } returns row

            val input =
                GetAccountQuery.Input(
                    targetAccountId = targetAccountId,
                    requestAccountId = "AZ0099", // 別人
                    isAdmin = true,
                )
            val output = query.execute(input)

            assertEquals("AZ0001", output.accountId)
            assertEquals("テストユーザー", output.name)
        }

        @Test
        fun `正常系： 非 admin が自分のアカウントを取得できる`() {
            val row = buildDetailRow()
            every { accountMapper.findAccountDetailById("AZ0001") } returns row

            val input =
                GetAccountQuery.Input(
                    targetAccountId = targetAccountId,
                    requestAccountId = "AZ0001", // 本人
                    isAdmin = false,
                )
            val output = query.execute(input)

            assertEquals("AZ0001", output.accountId)
        }

        @Test
        fun `正常系： deactivated アカウントの name と email が "***" にマスクされる`() {
            val row = buildDetailRow(name = "退会ユーザー", email = "quit@example.com", status = "deactivated")
            every { accountMapper.findAccountDetailById("AZ0001") } returns row

            val input =
                GetAccountQuery.Input(
                    targetAccountId = targetAccountId,
                    requestAccountId = "AZ0099",
                    isAdmin = true,
                )
            val output = query.execute(input)

            assertEquals("***", output.name)
            assertEquals("***", output.email)
        }

        @Test
        fun `正常系： ロール情報が正しく変換されて返る`() {
            val roles =
                listOf(
                    RoleRow(roleId = "01970000-0000-7000-8000-000000000001", code = "admin", name = "管理業務"),
                )
            val row = buildDetailRow(roles = roles)
            every { accountMapper.findAccountDetailById("AZ0001") } returns row

            val input =
                GetAccountQuery.Input(
                    targetAccountId = targetAccountId,
                    requestAccountId = "AZ0001",
                    isAdmin = true,
                )
            val output = query.execute(input)

            assertEquals(1, output.roles.size)
            assertEquals("admin", output.roles.first().code)
        }
    }

    @Nested
    inner class ErrorCases {
        @Test
        fun `異常系： 非 admin が他人のアカウントにアクセスすると ForbiddenOperationException`() {
            val input =
                GetAccountQuery.Input(
                    targetAccountId = targetAccountId,
                    requestAccountId = "AZ0099", // 別人
                    isAdmin = false,
                )
            assertThrows<ForbiddenOperationException> {
                query.execute(input)
            }
        }

        @Test
        fun `異常系： アカウントが存在しない場合は AccountNotFoundException`() {
            every { accountMapper.findAccountDetailById("AZ0001") } returns null

            val input =
                GetAccountQuery.Input(
                    targetAccountId = targetAccountId,
                    requestAccountId = "AZ0099",
                    isAdmin = true,
                )
            assertThrows<AccountNotFoundException> {
                query.execute(input)
            }
        }
    }
}
