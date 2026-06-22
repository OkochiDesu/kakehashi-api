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
        isAdmin: Boolean = true,
        admin: Boolean = true,
        viewPersonalInfo: Boolean = false,
        version: Int = 0,
    ) = AssignRolesUseCase.Input(
        targetAccountId = targetAccountId,
        operatorAccountId = operatorAccountId,
        isAdmin = isAdmin,
        admin = admin,
        viewPersonalInfo = viewPersonalInfo,
        version = version,
    )

    @Nested
    inner class NormalCases {
        @Test
        fun `正常系： admin=true viewPersonalInfo=false で admin ロールが付与される`() {
            fakeRepository.accounts["AZ0001"] = buildAccount(version = 0)

            val output = useCase.execute(buildInput(admin = true, viewPersonalInfo = false))

            assertEquals("AZ0001", output.accountId)
            assertEquals(1, output.roles.size)
            assertEquals(RoleCode.ADMIN.code, output.roles.first().code)
            assertEquals(1, fakeRepository.assignRolesAndBumpVersionCalls.size)
            assertEquals(
                listOf(AssignRolesUseCase.ADMIN_ROLE_ID),
                fakeRepository.assignRolesAndBumpVersionCalls.first().second,
            )
        }

        @Test
        fun `正常系： admin=true viewPersonalInfo=true で 2 ロールが付与される`() {
            fakeRepository.accounts["AZ0001"] = buildAccount(version = 0)

            val output = useCase.execute(buildInput(admin = true, viewPersonalInfo = true))

            assertEquals(2, output.roles.size)
        }

        @Test
        fun `正常系： admin=false viewPersonalInfo=false でロールが全剥奪される`() {
            fakeRepository.accounts["AZ0001"] = buildAccount(version = 0)

            val output = useCase.execute(buildInput(admin = false, viewPersonalInfo = false))

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
        fun `異常系： isAdmin=false は ForbiddenOperationException`() {
            assertThrows<ForbiddenOperationException> {
                useCase.execute(buildInput(isAdmin = false))
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
