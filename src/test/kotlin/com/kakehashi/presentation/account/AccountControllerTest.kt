package com.kakehashi.presentation.account

import com.kakehashi.domain.account.AccountId
import com.kakehashi.domain.account.AccountStatus
import com.kakehashi.presentation.GlobalExceptionHandler
import com.kakehashi.usecase.account.AssignRolesUseCase
import com.kakehashi.usecase.account.EditAccountUseCase
import com.kakehashi.usecase.account.GetAccountQuery
import com.kakehashi.usecase.account.GoogleSsoCallbackUseCase
import com.kakehashi.usecase.account.ListAccountsQuery
import com.kakehashi.usecase.account.RegisterAccountUseCase
import com.kakehashi.usecase.account.SuspendAccountUseCase
import com.kakehashi.usecase.account.UnsuspendAccountUseCase
import com.kakehashi.usecase.account.exception.AccountNotFoundException
import com.kakehashi.usecase.account.exception.ForbiddenOperationException
import com.kakehashi.usecase.account.exception.InvalidStatusTransitionException
import com.kakehashi.usecase.account.exception.OptimisticLockException
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.OffsetDateTime

/**
 * AccountController 結合テスト（WebMvc 層のみ）
 *
 * 設計書No：UC-A1/A3/A4/A5/A6/A7
 * ADRNo：APP-ADR-0009
 *
 * @WebMvcTest と @Nested inner class の組み合わせでは @MockkBean が内部クラスに届かないため
 * フラットな構造で記述する。
 *
 * ★★全体観点★★
 * HTTP レイヤー（ステータスコード・レスポンスフォーマット）と UseCase 呼び出しの橋渡しを検証する。
 * UseCase のビジネスロジックはここでは検証しない。
 * GlobalExceptionHandler による例外 → HTTP ステータスコードのマッピングも合わせて確認する。
 *
 * 《観　点》googleCallback: SSO コールバックの HTTP マッピング確認
 * 《テスト》googleCallback 正常系： PROVISIONAL アカウントは 200 OK を返す
 * 《テスト》googleCallback 正常系： ACTIVE アカウントは 200 OK を返す
 * 《テスト》googleCallback 異常系： SUSPENDED アカウントは 403 Forbidden を返す
 * 《テスト》googleCallback 異常系： DEACTIVATED アカウントは 403 Forbidden を返す
 * 《テスト》googleCallback 異常系： idToken がない場合は 400 Bad Request
 *
 * 《観　点》register: 登録完了リクエストの HTTP マッピング確認
 * 《テスト》register 正常系： 200 OK を返す
 * 《テスト》register 異常系： InvalidStatusTransitionException は 409 Conflict
 *
 * 《観　点》editMe: 表示名更新リクエストの HTTP マッピング確認
 * 《テスト》editMe 正常系： 200 OK を返す
 * 《テスト》editMe 異常系： OptimisticLockException は 409 Conflict
 *
 * 《観　点》assignRoles / suspend / unsuspend: 管理者操作の HTTP マッピング確認
 * 《テスト》assignRoles 正常系： X-Is-Admin=true で 200 OK を返す
 * 《テスト》suspend 正常系： X-Is-Admin=true で 200 OK を返す
 * 《テスト》unsuspend 正常系： X-Is-Admin=true で 200 OK を返す
 * 《テスト》assignRoles 異常系： X-Is-Admin=false で ForbiddenOperationException は 403 Forbidden
 * 《テスト》suspend 異常系： X-Is-Admin=false で ForbiddenOperationException は 403 Forbidden
 * 《テスト》unsuspend 異常系： X-Is-Admin=false で ForbiddenOperationException は 403 Forbidden
 *
 * 《観　点》listAccounts: 管理者・非管理者ともに 200 OK を返すことの確認
 * 《テスト》listAccounts 正常系： X-Is-Admin=true で 200 OK を返す
 * 《テスト》listAccounts 正常系： X-Is-Admin=false でも 200 OK（UseCase 内で active に強制）
 *
 * 《観　点》getAccount: 本人参照・他者参照・不在リソースの HTTP マッピング確認
 * 《テスト》getAccount 正常系： 本人が自分の ID で 200 OK
 * 《テスト》getAccount 異常系： 非 admin が他人の ID でアクセスすると ForbiddenOperationException は 403 Forbidden
 * 《テスト》getAccount 異常系： 存在しない accountId は 404 Not Found
 */
@WebMvcTest(AccountController::class)
@Import(GlobalExceptionHandler::class)
class AccountControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var googleSsoCallbackUseCase: GoogleSsoCallbackUseCase

    // registerAccountUseCase.execute の引数が @JvmInline value class (AccountId) のため
    // any() のシグネチャ生成で IllegalArgumentException が発生しないよう relaxed = true を使用する
    @MockkBean(relaxed = true)
    lateinit var registerAccountUseCase: RegisterAccountUseCase

    @MockkBean
    lateinit var editAccountUseCase: EditAccountUseCase

    @MockkBean
    lateinit var assignRolesUseCase: AssignRolesUseCase

    @MockkBean
    lateinit var suspendAccountUseCase: SuspendAccountUseCase

    @MockkBean
    lateinit var unsuspendAccountUseCase: UnsuspendAccountUseCase

    @MockkBean
    lateinit var listAccountsQuery: ListAccountsQuery

    @MockkBean
    lateinit var getAccountQuery: GetAccountQuery

    // ================================================================
    // UC-A1: POST /api/auth/google/callback
    // ================================================================

    @Test
    fun `googleCallback 正常系： PROVISIONAL アカウントは 200 OK を返す`() {
        every { googleSsoCallbackUseCase.execute(any()) } returns
            GoogleSsoCallbackUseCase.Output(
                accountId = "AZ0001",
                status = AccountStatus.PROVISIONAL,
                redirectTo = "/registration",
            )

        mockMvc
            .post("/api/auth/google/callback") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"idToken":"valid_token"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.accountId") { value("AZ0001") }
                jsonPath("$.redirectTo") { value("/registration") }
            }
    }

    @Test
    fun `googleCallback 正常系： ACTIVE アカウントは 200 OK を返す`() {
        every { googleSsoCallbackUseCase.execute(any()) } returns
            GoogleSsoCallbackUseCase.Output(
                accountId = "AZ0001",
                status = AccountStatus.ACTIVE,
                redirectTo = "/mypage",
            )

        mockMvc
            .post("/api/auth/google/callback") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"idToken":"valid_token"}"""
            }.andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `googleCallback 異常系： SUSPENDED アカウントは 403 Forbidden を返す`() {
        every { googleSsoCallbackUseCase.execute(any()) } returns
            GoogleSsoCallbackUseCase.Output(
                accountId = "AZ0001",
                status = AccountStatus.SUSPENDED,
                redirectTo = "/error/suspended",
            )

        mockMvc
            .post("/api/auth/google/callback") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"idToken":"valid_token"}"""
            }.andExpect {
                status { isForbidden() }
            }
    }

    @Test
    fun `googleCallback 異常系： DEACTIVATED アカウントは 403 Forbidden を返す`() {
        every { googleSsoCallbackUseCase.execute(any()) } returns
            GoogleSsoCallbackUseCase.Output(
                accountId = "AZ0001",
                status = AccountStatus.DEACTIVATED,
                redirectTo = "/error/deactivated",
            )

        mockMvc
            .post("/api/auth/google/callback") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"idToken":"valid_token"}"""
            }.andExpect {
                status { isForbidden() }
            }
    }

    @Test
    fun `googleCallback 異常系： idToken がない場合は 400 Bad Request`() {
        mockMvc
            .post("/api/auth/google/callback") {
                contentType = MediaType.APPLICATION_JSON
                content = """{}"""
            }.andExpect {
                status { isBadRequest() }
            }
    }

    // ================================================================
    // UC-A3: POST /api/accounts/me/registration
    // ================================================================

    @Test
    fun `register 正常系： 200 OK を返す`() {
        // @JvmInline value class (AccountId) は any() でのシグネチャ生成に失敗するため具体値で指定する
        every { registerAccountUseCase.execute(AccountId("AZ0001")) } returns
            RegisterAccountUseCase.Output(
                accountId = "AZ0001",
                status = AccountStatus.ACTIVE,
            )

        mockMvc
            .post("/api/accounts/me/registration") {
                header("X-Account-Id", "AZ0001")
            }.andExpect {
                status { isOk() }
                jsonPath("$.accountId") { value("AZ0001") }
                jsonPath("$.status") { value("active") }
            }
    }

    @Test
    fun `register 異常系： InvalidStatusTransitionException は 409 Conflict`() {
        // @JvmInline value class (AccountId) は any() でのシグネチャ生成に失敗するため具体値で指定する
        every { registerAccountUseCase.execute(AccountId("AZ0001")) } throws
            InvalidStatusTransitionException(
                accountId = "AZ0001",
                from = AccountStatus.ACTIVE,
                to = AccountStatus.ACTIVE,
            )

        mockMvc
            .post("/api/accounts/me/registration") {
                header("X-Account-Id", "AZ0001")
            }.andExpect {
                status { isConflict() }
                jsonPath("$.code") { value("INVALID_STATUS_TRANSITION") }
            }
    }

    // ================================================================
    // UC-A4: PATCH /api/accounts/me
    // ================================================================

    @Test
    fun `editMe 正常系： 200 OK を返す`() {
        every { editAccountUseCase.execute(any()) } returns
            EditAccountUseCase.Output(
                accountId = "AZ0001",
                name = "新しい名前",
                email = "user@example.com",
                status = AccountStatus.ACTIVE,
                version = 1,
            )

        mockMvc
            .patch("/api/accounts/me") {
                header("X-Account-Id", "AZ0001")
                contentType = MediaType.APPLICATION_JSON
                content = """{"name":"新しい名前","version":0}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.name") { value("新しい名前") }
                jsonPath("$.version") { value(1) }
            }
    }

    @Test
    fun `editMe 異常系： OptimisticLockException は 409 Conflict`() {
        every { editAccountUseCase.execute(any()) } throws
            OptimisticLockException(
                accountId = "AZ0001",
                requestVersion = 0,
                currentVersion = 1,
            )

        mockMvc
            .patch("/api/accounts/me") {
                header("X-Account-Id", "AZ0001")
                contentType = MediaType.APPLICATION_JSON
                content = """{"name":"新しい名前","version":0}"""
            }.andExpect {
                status { isConflict() }
                jsonPath("$.code") { value("OPTIMISTIC_LOCK_CONFLICT") }
            }
    }

    // ================================================================
    // UC-A6: PUT /api/accounts/{accountId}/roles
    // ================================================================

    @Test
    fun `assignRoles 正常系： X-Is-Admin=true で 200 OK を返す`() {
        every { assignRolesUseCase.execute(any()) } returns
            AssignRolesUseCase.Output(
                accountId = "AZ0001",
                roles = emptyList(),
                version = 1,
            )

        mockMvc
            .put("/api/accounts/AZ0001/roles") {
                header("X-Account-Id", "AZ0002")
                header("X-Is-Admin", "true")
                contentType = MediaType.APPLICATION_JSON
                content = """{"admin":false,"viewPersonalInfo":false,"version":0}"""
            }.andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `assignRoles 異常系： X-Is-Admin=false で ForbiddenOperationException は 403 Forbidden`() {
        every { assignRolesUseCase.execute(any()) } throws
            ForbiddenOperationException("Only admin users can assign roles")

        mockMvc
            .put("/api/accounts/AZ0001/roles") {
                header("X-Account-Id", "AZ0002")
                header("X-Is-Admin", "false")
                contentType = MediaType.APPLICATION_JSON
                content = """{"admin":false,"viewPersonalInfo":false,"version":0}"""
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.code") { value("FORBIDDEN") }
            }
    }

    // ================================================================
    // UC-A7: POST /api/accounts/{accountId}/suspend
    // ================================================================

    @Test
    fun `suspend 正常系： X-Is-Admin=true で 200 OK を返す`() {
        every { suspendAccountUseCase.execute(any()) } returns
            SuspendAccountUseCase.Output(
                accountId = "AZ0001",
                status = AccountStatus.SUSPENDED,
                suspendedAt = OffsetDateTime.parse("2026-01-01T00:00:00+09:00"),
                version = 1,
            )

        mockMvc
            .post("/api/accounts/AZ0001/suspend") {
                header("X-Account-Id", "AZ0002")
                header("X-Is-Admin", "true")
                contentType = MediaType.APPLICATION_JSON
                content = """{"version":0}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.status") { value("suspended") }
            }
    }

    @Test
    fun `suspend 異常系： X-Is-Admin=false で ForbiddenOperationException は 403 Forbidden`() {
        every { suspendAccountUseCase.execute(any()) } throws
            ForbiddenOperationException("Only admin users can suspend accounts")

        mockMvc
            .post("/api/accounts/AZ0001/suspend") {
                header("X-Account-Id", "AZ0002")
                header("X-Is-Admin", "false")
                contentType = MediaType.APPLICATION_JSON
                content = """{"version":0}"""
            }.andExpect {
                status { isForbidden() }
            }
    }

    // ================================================================
    // UC-A7: POST /api/accounts/{accountId}/unsuspend
    // ================================================================

    @Test
    fun `unsuspend 正常系： X-Is-Admin=true で 200 OK を返す`() {
        every { unsuspendAccountUseCase.execute(any()) } returns
            UnsuspendAccountUseCase.Output(
                accountId = "AZ0001",
                status = AccountStatus.ACTIVE,
                suspendedAt = null,
                version = 1,
            )

        mockMvc
            .post("/api/accounts/AZ0001/unsuspend") {
                header("X-Account-Id", "AZ0002")
                header("X-Is-Admin", "true")
                contentType = MediaType.APPLICATION_JSON
                content = """{"version":0}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.status") { value("active") }
            }
    }

    @Test
    fun `unsuspend 異常系： X-Is-Admin=false で ForbiddenOperationException は 403 Forbidden`() {
        every { unsuspendAccountUseCase.execute(any()) } throws
            ForbiddenOperationException("Only admin users can unsuspend accounts")

        mockMvc
            .post("/api/accounts/AZ0001/unsuspend") {
                header("X-Account-Id", "AZ0002")
                header("X-Is-Admin", "false")
                contentType = MediaType.APPLICATION_JSON
                content = """{"version":0}"""
            }.andExpect {
                status { isForbidden() }
            }
    }

    // ================================================================
    // UC-A5: GET /api/accounts
    // ================================================================

    @Test
    fun `listAccounts 正常系： X-Is-Admin=true で 200 OK を返す`() {
        every { listAccountsQuery.execute(any()) } returns
            ListAccountsQuery.Output(
                content = emptyList(),
                totalElements = 0L,
                totalPages = 0,
                page = 0,
                size = 20,
            )

        mockMvc
            .get("/api/accounts") {
                header("X-Account-Id", "AZ0001")
                header("X-Is-Admin", "true")
            }.andExpect {
                status { isOk() }
                jsonPath("$.totalElements") { value(0) }
            }
    }

    @Test
    fun `listAccounts 正常系： X-Is-Admin=false でも 200 OK（UseCase 内で active に強制）`() {
        every { listAccountsQuery.execute(any()) } returns
            ListAccountsQuery.Output(
                content = emptyList(),
                totalElements = 0L,
                totalPages = 0,
                page = 0,
                size = 20,
            )

        // Controller 自体は非 admin を弾かない。UseCase が status を active に強制する
        mockMvc
            .get("/api/accounts") {
                header("X-Account-Id", "AZ0001")
                header("X-Is-Admin", "false")
            }.andExpect {
                status { isOk() }
            }
    }

    // ================================================================
    // GET /api/accounts/{accountId}
    // ================================================================

    @Test
    fun `getAccount 正常系： 本人が自分の ID で 200 OK`() {
        every { getAccountQuery.execute(any()) } returns
            GetAccountQuery.Output(
                accountId = "AZ0001",
                name = "テストユーザー",
                email = "user@example.com",
                status = "active",
                roles = emptyList(),
                suspendedAt = null,
                version = 0,
                createdAt = "2026-01-01T00:00:00+09:00",
                updatedAt = "2026-01-01T00:00:00+09:00",
                updatedBy = "AZ0001",
            )

        mockMvc
            .get("/api/accounts/AZ0001") {
                header("X-Account-Id", "AZ0001")
                header("X-Is-Admin", "false")
            }.andExpect {
                status { isOk() }
                jsonPath("$.accountId") { value("AZ0001") }
            }
    }

    @Test
    fun `getAccount 異常系： 非 admin が他人の ID でアクセスすると ForbiddenOperationException は 403 Forbidden`() {
        every { getAccountQuery.execute(any()) } throws
            ForbiddenOperationException("Account AZ0001 is not allowed to access AZ0002")

        mockMvc
            .get("/api/accounts/AZ0002") {
                header("X-Account-Id", "AZ0001")
                header("X-Is-Admin", "false")
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.code") { value("FORBIDDEN") }
            }
    }

    @Test
    fun `getAccount 異常系： 存在しない accountId は 404 Not Found`() {
        every { getAccountQuery.execute(any()) } throws AccountNotFoundException("AZ9999")

        mockMvc
            .get("/api/accounts/AZ9999") {
                header("X-Account-Id", "AZ0001")
                header("X-Is-Admin", "true")
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("ACCOUNT_NOT_FOUND") }
            }
    }
}
