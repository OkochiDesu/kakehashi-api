package com.kakehashi.presentation.account

import com.kakehashi.domain.account.AccountId
import com.kakehashi.domain.account.AccountStatus
import com.kakehashi.domain.account.RoleCode
import com.kakehashi.usecase.account.AssignRolesUseCase
import com.kakehashi.usecase.account.EditAccountUseCase
import com.kakehashi.usecase.account.GetAccountQuery
import com.kakehashi.usecase.account.GoogleSsoCallbackUseCase
import com.kakehashi.usecase.account.ListAccountsQuery
import com.kakehashi.usecase.account.RegisterAccountUseCase
import com.kakehashi.usecase.account.SuspendAccountUseCase
import com.kakehashi.usecase.account.UnsuspendAccountUseCase
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * アカウント・ロード コントローラ
 *
 * 根拠: docs/design/api/account-role.md（全エンドポイント）
 * APP-ADR-0009: API パスにバージョンプレフィックスを含めない（/api/accounts/...）
 *
 * 認証（暫定実装）:
 * - Spring Security の本格実装は別ブランチで行う
 * - @RequestHeader("X-Account-Id") を仮の認証方式として使用する
 * - admin 権限チェックは UseCase 内で account_roles を確認する形で将来実装する
 *   現時点では @RequestHeader("X-Is-Admin") で仮受けする
 */
@RestController
@RequestMapping("/api")
class AccountController(
    private val googleSsoCallbackUseCase: GoogleSsoCallbackUseCase,
    private val registerAccountUseCase: RegisterAccountUseCase,
    private val editAccountUseCase: EditAccountUseCase,
    private val assignRolesUseCase: AssignRolesUseCase,
    private val suspendAccountUseCase: SuspendAccountUseCase,
    private val unsuspendAccountUseCase: UnsuspendAccountUseCase,
    private val listAccountsQuery: ListAccountsQuery,
    private val getAccountQuery: GetAccountQuery,
) {
    // ================================================================
    // UC-A1: Google SSO コールバック・JIT プロビジョニング
    // POST /api/auth/google/callback
    // ================================================================

    /**
     * Google SSO コールバックを受け付け、JIT プロビジョニングを行う。
     *
     * 設計書No：UC-A1
     * ADRNo：APP-ADR-0009
     *
     * suspended / deactivated のアカウントは 403 Forbidden を返す（ログイン拒否）。
     */
    @PostMapping("/auth/google/callback")
    fun googleCallback(
        @Valid @RequestBody body: GoogleCallbackRequest,
    ): ResponseEntity<GoogleCallbackResponse> {
        // 暫定: id_token の検証・sub ハッシュ化は本格実装まで stub
        // 実際の実装では GoogleIdTokenVerifier 等で検証し sub をハッシュ化する
        val input =
            GoogleSsoCallbackUseCase.Input(
                googleSubHash = body.idToken.take(64), // stub: 本来は SHA-256 ハッシュ
                email = "stub@example.com", // stub: id_token から取得
                name = "Stub User", // stub: id_token から取得
            )
        val output = googleSsoCallbackUseCase.execute(input)

        // 設計書 UC-A1: suspended / deactivated のアカウントのログイン試行は 403 Forbidden
        if (output.status == AccountStatus.SUSPENDED || output.status == AccountStatus.DEACTIVATED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        return ResponseEntity.ok(
            GoogleCallbackResponse(
                accountId = output.accountId,
                status = output.status.toDbValue(),
                redirectTo = output.redirectTo,
            ),
        )
    }

    // ================================================================
    // UC-A3: 本登録申込み
    // POST /api/accounts/me/registration
    // ================================================================

    /**
     * 本登録申込みを受け付け、PROVISIONAL → ACTIVE へ遷移させる。
     *
     * 設計書No：UC-A3
     * ADRNo：APP-ADR-0009
     */
    @PostMapping("/accounts/me/registration")
    fun register(
        @RequestHeader("X-Account-Id") accountId: String,
    ): ResponseEntity<RegisterResponse> {
        val output = registerAccountUseCase.execute(AccountId(accountId))
        return ResponseEntity.ok(
            RegisterResponse(
                accountId = output.accountId,
                status = output.status.toDbValue(),
            ),
        )
    }

    // ================================================================
    // UC-A4: アカウント情報編集（本人）
    // PATCH /api/accounts/me
    // ================================================================

    /**
     * 本人のアカウント表示名を更新する。
     *
     * 設計書No：UC-A4
     * ADRNo：APP-ADR-0009
     */
    @PatchMapping("/accounts/me")
    fun editMe(
        @RequestHeader("X-Account-Id") accountId: String,
        @Valid @RequestBody body: EditAccountRequest,
    ): ResponseEntity<EditAccountResponse> {
        val output =
            editAccountUseCase.execute(
                EditAccountUseCase.Input(
                    accountId = AccountId(accountId),
                    name = body.name,
                    version = body.version,
                ),
            )
        return ResponseEntity.ok(
            EditAccountResponse(
                accountId = output.accountId,
                name = output.name,
                email = output.email,
                status = output.status.toDbValue(),
                version = output.version,
            ),
        )
    }

    // ================================================================
    // UC-A5: アカウント一覧・検索（管理者）
    // GET /api/accounts
    // ================================================================

    /**
     * アカウント一覧を検索して返す（管理者向け）。
     *
     * 設計書No：UC-A5
     * ADRNo：APP-ADR-0009
     *
     * 非管理者からのリクエストは ForbiddenOperationException を発生させないが、
     * UseCase 内で status フィルタを active に強制する。
     */
    @GetMapping("/accounts")
    fun listAccounts(
        @RequestHeader("X-Account-Id") accountId: String,
        @RequestHeader("X-Is-Admin", defaultValue = "false") isAdmin: Boolean,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) status: List<String>?,
        @RequestParam(required = false) roleCode: String?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) size: Int,
    ): ResponseEntity<ListAccountsResponse> {
        val output =
            listAccountsQuery.execute(
                ListAccountsQuery.Input(
                    name = name,
                    statuses = status,
                    roleCode = roleCode,
                    page = page,
                    size = size,
                    isAdmin = isAdmin,
                ),
            )
        return ResponseEntity.ok(
            ListAccountsResponse(
                content =
                    output.content.map { a ->
                        AccountSummaryResponse(
                            accountId = a.accountId,
                            name = a.name,
                            status = a.status,
                        )
                    },
                totalElements = output.totalElements,
                totalPages = output.totalPages,
                page = output.page,
                size = output.size,
            ),
        )
    }

    // ================================================================
    // アカウント詳細取得
    // GET /api/accounts/{accountId}
    // ================================================================

    /**
     * アカウント詳細を取得する。
     *
     * 設計書No：-
     * ADRNo：APP-ADR-0009
     *
     * 非管理者は自分のアカウントのみ参照可能（他人のアカウントは ForbiddenOperationException → 403）。
     */
    @GetMapping("/accounts/{accountId}")
    fun getAccount(
        @RequestHeader("X-Account-Id") requestAccountId: String,
        @RequestHeader("X-Is-Admin", defaultValue = "false") isAdmin: Boolean,
        @PathVariable accountId: String,
    ): ResponseEntity<AccountDetailResponse> {
        val output =
            getAccountQuery.execute(
                GetAccountQuery.Input(
                    targetAccountId = AccountId(accountId),
                    requestAccountId = requestAccountId,
                    isAdmin = isAdmin,
                ),
            )
        return ResponseEntity.ok(
            AccountDetailResponse(
                accountId = output.accountId,
                name = output.name,
                email = output.email,
                status = output.status,
                roles =
                    output.roles.map { r ->
                        RoleResponse(
                            roleId = r.roleId,
                            code = r.code,
                            name = r.name,
                        )
                    },
                suspendedAt = output.suspendedAt,
                version = output.version,
                createdAt = output.createdAt,
                updatedAt = output.updatedAt,
                updatedBy = output.updatedBy,
            ),
        )
    }

    // ================================================================
    // UC-A6: 権限付与・変更（管理者）
    // PUT /api/accounts/{accountId}/roles
    // ================================================================

    /**
     * 対象アカウントのロールを全置換する（管理者のみ実行可能）。
     *
     * 設計書No：UC-A6
     * ADRNo：APP-ADR-0009
     */
    @PutMapping("/accounts/{accountId}/roles")
    fun assignRoles(
        @RequestHeader("X-Account-Id") requestAccountId: String,
        @RequestHeader("X-Is-Admin", defaultValue = "false") isAdmin: Boolean,
        @PathVariable accountId: String,
        @Valid @RequestBody body: AssignRolesRequest,
    ): ResponseEntity<AssignRolesResponse> {
        val output =
            assignRolesUseCase.execute(
                AssignRolesUseCase.Input(
                    targetAccountId = AccountId(accountId),
                    operatorAccountId = requestAccountId,
                    operatorIsAdmin = isAdmin,
                    grantAdminRole = body.admin,
                    grantViewPersonalInfoRole = body.viewPersonalInfo,
                    version = body.version,
                ),
            )
        return ResponseEntity.ok(
            AssignRolesResponse(
                accountId = output.accountId,
                roles =
                    output.roles.map { r ->
                        RoleResponse(
                            roleId = r.roleId,
                            code = r.code,
                            name = r.name,
                        )
                    },
                version = output.version,
            ),
        )
    }

    // ================================================================
    // UC-A7: アカウント停止（管理者）
    // POST /api/accounts/{accountId}/suspend
    // ================================================================

    /**
     * 対象アカウントを停止する（管理者のみ実行可能）。
     *
     * 設計書No：UC-A7
     * ADRNo：APP-ADR-0009
     */
    @PostMapping("/accounts/{accountId}/suspend")
    fun suspendAccount(
        @RequestHeader("X-Account-Id") requestAccountId: String,
        @RequestHeader("X-Is-Admin", defaultValue = "false") isAdmin: Boolean,
        @PathVariable accountId: String,
        @Valid @RequestBody body: VersionRequest,
    ): ResponseEntity<SuspendResponse> {
        val output =
            suspendAccountUseCase.execute(
                SuspendAccountUseCase.Input(
                    targetAccountId = AccountId(accountId),
                    operatorAccountId = requestAccountId,
                    isAdmin = isAdmin,
                    version = body.version,
                ),
            )
        return ResponseEntity.ok(
            SuspendResponse(
                accountId = output.accountId,
                status = output.status.toDbValue(),
                suspendedAt = output.suspendedAt.toString(),
                version = output.version,
            ),
        )
    }

    // ================================================================
    // UC-A7: アカウント停止解除（管理者）
    // POST /api/accounts/{accountId}/unsuspend
    // ================================================================

    /**
     * 対象アカウントの停止を解除する（管理者のみ実行可能）。
     *
     * 設計書No：UC-A7
     * ADRNo：APP-ADR-0009
     */
    @PostMapping("/accounts/{accountId}/unsuspend")
    fun unsuspendAccount(
        @RequestHeader("X-Account-Id") requestAccountId: String,
        @RequestHeader("X-Is-Admin", defaultValue = "false") isAdmin: Boolean,
        @PathVariable accountId: String,
        @Valid @RequestBody body: VersionRequest,
    ): ResponseEntity<UnsuspendResponse> {
        val output =
            unsuspendAccountUseCase.execute(
                UnsuspendAccountUseCase.Input(
                    targetAccountId = AccountId(accountId),
                    operatorAccountId = requestAccountId,
                    isAdmin = isAdmin,
                    version = body.version,
                ),
            )
        return ResponseEntity.ok(
            UnsuspendResponse(
                accountId = output.accountId,
                status = output.status.toDbValue(),
                suspendedAt = null,
                version = output.version,
            ),
        )
    }
}

// ================================================================
// リクエスト DTO
// ================================================================

data class GoogleCallbackRequest(
    @field:NotBlank val idToken: String,
)

data class EditAccountRequest(
    @field:NotBlank val name: String,
    @field:NotNull val version: Int,
)

data class AssignRolesRequest(
    @field:NotNull val admin: Boolean,
    @field:NotNull val viewPersonalInfo: Boolean,
    @field:NotNull val version: Int,
) {
    // 全権限フラグが null でないことを確認（設計書: 全権限を必ず指定）
    fun toRoleCodes(): Set<RoleCode> =
        buildSet {
            if (admin) add(RoleCode.ADMIN)
            if (viewPersonalInfo) add(RoleCode.VIEW_PERSONAL_INFO)
        }
}

data class VersionRequest(
    @field:NotNull val version: Int,
)

// ================================================================
// レスポンス DTO
// ================================================================

data class GoogleCallbackResponse(
    val accountId: String,
    val status: String,
    val redirectTo: String,
)

data class RegisterResponse(
    val accountId: String,
    val status: String,
)

data class EditAccountResponse(
    val accountId: String,
    val name: String,
    val email: String,
    val status: String,
    val version: Int,
)

data class AccountSummaryResponse(
    val accountId: String,
    val name: String,
    val status: String,
)

data class ListAccountsResponse(
    val content: List<AccountSummaryResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int,
)

data class RoleResponse(
    val roleId: String,
    val code: String,
    val name: String,
)

data class AccountDetailResponse(
    val accountId: String,
    val name: String,
    val email: String,
    val status: String,
    val roles: List<RoleResponse>,
    val suspendedAt: String?,
    val version: Int,
    val createdAt: String,
    val updatedAt: String,
    val updatedBy: String,
)

data class AssignRolesResponse(
    val accountId: String,
    val roles: List<RoleResponse>,
    val version: Int,
)

data class SuspendResponse(
    val accountId: String,
    val status: String,
    val suspendedAt: String,
    val version: Int,
)

data class UnsuspendResponse(
    val accountId: String,
    val status: String,
    val suspendedAt: String?,
    val version: Int,
)
