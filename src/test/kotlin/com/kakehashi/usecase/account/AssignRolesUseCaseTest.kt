package com.kakehashi.usecase.account

import com.kakehashi.domain.account.AccountId
import com.kakehashi.domain.account.RoleCode
import com.kakehashi.usecase.account.AccountTestFixtures.buildAccount
import com.kakehashi.usecase.account.exception.AccountNotFoundException
import com.kakehashi.usecase.account.exception.ForbiddenOperationException
import com.kakehashi.usecase.account.exception.OptimisticLockException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.OffsetDateTime

/**
 * AssignRolesUseCase 単体テスト
 *
 * 設計書No：UC-A6
 * ADRNo：APP-ADR-0005, APP-ADR-0007, APP-ADR-0008
 *
 * MockK の @JvmInline value class (AccountId) のシグネチャ生成に問題があるため
 * FakeAccountRepository を使用する。
 */
class AssignRolesUseCaseTest {
    private lateinit var fakeRepository: FakeAccountRepository
    private lateinit var useCase: AssignRolesUseCase

    private val targetAccountId = AccountId("AZ0001")
    private val operatorAccountId = "AZ0002"

    @BeforeEach
    fun setUp() {
        fakeRepository = FakeAccountRepository()
        useCase = AssignRolesUseCase(fakeRepository)
    }

    private fun buildInput(
        operatorIsAdmin: Boolean = true,
        grantAdminRole: Boolean = true,
        grantViewPersonalInfoRole: Boolean = false,
        version: Int = 0,
    ) = AssignRolesUseCase.Input(
        targetAccountId = targetAccountId,
        operatorAccountId = operatorAccountId,
        operatorIsAdmin = operatorIsAdmin,
        grantAdminRole = grantAdminRole,
        grantViewPersonalInfoRole = grantViewPersonalInfoRole,
        version = version,
    )

    @Nested
    inner class NormalCases {
        @Test
        fun `正常系： grantAdminRole=true grantViewPersonalInfoRole=false で admin ロールが付与される`() {
            val before = OffsetDateTime.now().minusSeconds(1)
            fakeRepository.accounts["AZ0001"] = buildAccount(version = 0)

            val output = useCase.execute(buildInput(grantAdminRole = true, grantViewPersonalInfoRole = false))

            assertEquals("AZ0001", output.accountId)
            assertEquals(1, output.roles.size)
            assertEquals(RoleCode.ADMIN.code, output.roles.first().code)
            assertEquals(1, fakeRepository.assignRolesAndBumpVersionCalls.size)
            assertEquals(
                listOf(AssignRolesUseCase.roleIdFor(RoleCode.ADMIN)),
                fakeRepository.assignRolesAndBumpVersionCalls.first().second,
                "admin ロールの UUID がリポジトリに渡されること",
            )
            // 監査カラム: updatedAt・updatedBy が更新されていることを確認
            val saved = fakeRepository.accounts["AZ0001"]!!
            assertTrue(saved.updatedAt.isAfter(before)) { "updatedAt が更新されていません: ${saved.updatedAt}" }
            assertEquals(operatorAccountId, saved.updatedBy)
        }

        @Test
        fun `正常系： grantAdminRole=true grantViewPersonalInfoRole=true で 2 ロールが付与される`() {
            fakeRepository.accounts["AZ0001"] = buildAccount(version = 0)

            val output = useCase.execute(buildInput(grantAdminRole = true, grantViewPersonalInfoRole = true))

            assertEquals(2, output.roles.size)
        }

        @Test
        fun `正常系： grantAdminRole=false grantViewPersonalInfoRole=false でロールが全剥奪される`() {
            fakeRepository.accounts["AZ0001"] = buildAccount(version = 0)

            val output = useCase.execute(buildInput(grantAdminRole = false, grantViewPersonalInfoRole = false))

            assertTrue(output.roles.isEmpty())
            assertTrue(
                fakeRepository.assignRolesAndBumpVersionCalls
                    .first()
                    .second
                    .isEmpty(),
            )
        }
    }

    @Nested
    inner class ErrorCases {
        @Test
        fun `異常系： operatorIsAdmin=false は ForbiddenOperationException`() {
            assertThrows<ForbiddenOperationException> {
                useCase.execute(buildInput(operatorIsAdmin = false))
            }
        }

        @Test
        fun `異常系： アカウントが存在しない場合は AccountNotFoundException`() {
            // fakeRepository にアカウントを登録しない
            assertThrows<AccountNotFoundException> {
                useCase.execute(buildInput())
            }
        }

        @Test
        fun `異常系： version 不一致は OptimisticLockException`() {
            // DB には version=1 が存在するが、リクエストは version=0
            fakeRepository.accounts["AZ0001"] = buildAccount(version = 1)

            assertThrows<OptimisticLockException> {
                useCase.execute(buildInput(version = 0))
            }
        }

        @Test
        fun `異常系： assignRolesAndBumpVersion が 0件（楽観ロック競合）は OptimisticLockException`() {
            fakeRepository.accounts["AZ0001"] = buildAccount(version = 0)
            fakeRepository.assignRolesAndBumpVersionResult = 0

            assertThrows<OptimisticLockException> {
                useCase.execute(buildInput())
            }
        }
    }
}
