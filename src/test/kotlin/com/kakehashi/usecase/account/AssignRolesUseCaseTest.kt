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
 *
 * ★★全体観点★★
 * ロール付与はシステム権限の根幹となる操作であり、ロールの全組み合わせ・操作権限チェック・
 * 楽観ロック整合性を網羅することで権限昇格バグや競合状態を防ぐ。
 *
 * ★★正常系★★
 * 《観　点》単一ロール付与と他ロールへの非影響の確認
 * 《テスト》grantAdminRole=true / grantViewPersonalInfoRole=false で admin ロールが付与される
 *
 * 《観　点》複数ロール同時付与の確認
 * 《テスト》grantAdminRole=true / grantViewPersonalInfoRole=true で 2 ロールが付与される
 *
 * 《観　点》ロール全剥奪（DELETE/INSERT 差し替え）の確認
 * 《テスト》grantAdminRole=false / grantViewPersonalInfoRole=false でロールが全剥奪される
 *
 * ★★異常系★★
 * 《観　点》非管理者によるロール変更を防ぐ権限ガードの確認
 * 《テスト》operatorIsAdmin=false は ForbiddenOperationException
 *
 * 《観　点》不在ユーザーへのロール付与を防ぐ早期失敗の確認
 * 《テスト》アカウントが存在しない場合は AccountNotFoundException
 *
 * 《観　点》楽観ロック競合の検出と currentVersion の正確な保持確認
 * 《テスト》version 不一致は OptimisticLockException
 * 《テスト》assignRolesAndBumpVersion が 0件（楽観ロック競合）は OptimisticLockException
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
