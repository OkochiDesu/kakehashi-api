package com.kakehashi.infrastructure.account

import com.kakehashi.domain.account.Account
import com.kakehashi.domain.account.AccountId
import com.kakehashi.domain.account.AccountRepository
import com.kakehashi.domain.account.AccountStatus
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

/**
 * AccountRepository 実装（Command 系 DB アクセス）
 *
 * 根拠: docs/architecture/package-structure.md（infrastructure 層の責務）
 * APP-ADR-0008: Command 側は集約 → Repository → DB の流れ
 * APP-ADR-0005: UPDATE 時に WHERE version = ? を条件に含め、0件更新なら 409 Conflict
 *
 * JdbcClient (Spring 6.1+) を使用。MyBatis は Query 系（AccountMapper）のみ。
 */
@Repository
class AccountRepositoryImpl(
    private val jdbcClient: JdbcClient,
) : AccountRepository {
    /**
     * アカウントを ID で取得する。
     *
     * 設計書No：UC-A3/A4/A6/A7
     * ADRNo：APP-ADR-0005
     */
    override fun findById(accountId: AccountId): Account? =
        jdbcClient
            .sql(
                """
                SELECT account_id, google_sub_hash, email, name, status, suspended_at,
                       version, created_by, updated_by, created_at, updated_at
                FROM accounts
                WHERE account_id = :accountId
                """.trimIndent(),
            ).param("accountId", accountId.value)
            .query { rs, _ ->
                Account(
                    accountId = AccountId(rs.getString("account_id")),
                    googleSubHash = rs.getString("google_sub_hash"),
                    email = rs.getString("email"),
                    name = rs.getString("name"),
                    status = AccountStatus.fromDbValue(rs.getString("status")),
                    suspendedAt = rs.getObject("suspended_at", OffsetDateTime::class.java),
                    version = rs.getInt("version"),
                    createdBy = rs.getString("created_by"),
                    updatedBy = rs.getString("updated_by"),
                    createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
                    updatedAt = rs.getObject("updated_at", OffsetDateTime::class.java),
                )
            }.optional()
            .orElse(null)

    /**
     * Google sub ハッシュでアカウントを取得する（ログイン照合用）。
     *
     * 設計書No：UC-A1
     * ADRNo：APP-ADR-0005
     */
    override fun findByGoogleSubHash(googleSubHash: String): Account? =
        jdbcClient
            .sql(
                """
                SELECT account_id, google_sub_hash, email, name, status, suspended_at,
                       version, created_by, updated_by, created_at, updated_at
                FROM accounts
                WHERE google_sub_hash = :googleSubHash
                """.trimIndent(),
            ).param("googleSubHash", googleSubHash)
            .query { rs, _ ->
                Account(
                    accountId = AccountId(rs.getString("account_id")),
                    googleSubHash = rs.getString("google_sub_hash"),
                    email = rs.getString("email"),
                    name = rs.getString("name"),
                    status = AccountStatus.fromDbValue(rs.getString("status")),
                    suspendedAt = rs.getObject("suspended_at", OffsetDateTime::class.java),
                    version = rs.getInt("version"),
                    createdBy = rs.getString("created_by"),
                    updatedBy = rs.getString("updated_by"),
                    createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
                    updatedAt = rs.getObject("updated_at", OffsetDateTime::class.java),
                )
            }.optional()
            .orElse(null)

    /**
     * アカウントを新規保存する（仮登録 UC-A1）。
     *
     * 設計書No：UC-A1
     * ADRNo：APP-ADR-0005
     */
    @Transactional
    override fun save(account: Account) {
        jdbcClient
            .sql(
                """
                INSERT INTO accounts
                    (account_id, google_sub_hash, email, name, status, suspended_at,
                     version, created_by, updated_by, created_at, updated_at)
                VALUES
                    (:accountId, :googleSubHash, :email, :name, :status, :suspendedAt,
                     :version, :createdBy, :updatedBy, :createdAt, :updatedAt)
                """.trimIndent(),
            ).param("accountId", account.accountId.value)
            .param("googleSubHash", account.googleSubHash)
            .param("email", account.email)
            .param("name", account.name)
            .param("status", account.status.toDbValue())
            .param("suspendedAt", account.suspendedAt)
            .param("version", account.version)
            .param("createdBy", account.createdBy)
            .param("updatedBy", account.updatedBy)
            .param("createdAt", account.createdAt)
            .param("updatedAt", account.updatedAt)
            .update()
    }

    /**
     * APP-ADR-0005: WHERE version = :version で楽観ロックを実装する
     * 更新件数 0 = version 不一致（呼び出し元で 409 Conflict に変換）
     */
    @Transactional
    override fun update(account: Account): Int =
        jdbcClient
            .sql(
                """
                UPDATE accounts
                SET email       = :email,
                    name        = :name,
                    status      = :status,
                    suspended_at = :suspendedAt,
                    version     = :version,
                    updated_by  = :updatedBy,
                    updated_at  = :updatedAt
                WHERE account_id = :accountId
                  AND version    = :prevVersion
                """.trimIndent(),
            ).param("email", account.email)
            .param("name", account.name)
            .param("status", account.status.toDbValue())
            .param("suspendedAt", account.suspendedAt)
            .param("version", account.version)
            .param("updatedBy", account.updatedBy)
            .param("updatedAt", account.updatedAt)
            .param("accountId", account.accountId.value)
            .param("prevVersion", account.version - 1)
            .update()

    /**
     * PostgreSQL シーケンスから次の account_id 連番を取得する。
     *
     * 設計書No：UC-A1
     * ADRNo：APP-ADR-0005
     */
    override fun nextAccountIdSequence(): Long =
        jdbcClient
            .sql("SELECT nextval('accounts_account_id_seq')")
            .query(Long::class.java)
            .single()

    /**
     * 対象アカウントが保持する role_id 一覧を取得する。
     *
     * 設計書No：UC-A6
     * ADRNo：APP-ADR-0005
     */
    override fun findRoleIdsByAccountId(accountId: AccountId): Set<UUID> =
        jdbcClient
            .sql("SELECT role_id FROM account_roles WHERE account_id = :accountId")
            .param("accountId", accountId.value)
            .query(UUID::class.java)
            .list()
            .filterNotNull()
            .toSet()

    /**
     * account_roles 全置換と accounts.version インクリメントを1トランザクションで実行する（UC-A6: 修正4）
     *
     * 根拠: code-reviewer REQUIRES_CHANGES 修正4
     * replaceRoles + update を別トランザクションで呼ぶと中間不整合が発生するため、
     * このメソッド内でまとめて @Transactional を付与する。
     */
    @Transactional
    override fun assignRolesAndBumpVersion(
        accountId: AccountId,
        roleIds: List<UUID>,
        account: Account,
        operatorId: String,
    ): Int {
        // 1. account_roles 全置換
        jdbcClient
            .sql("DELETE FROM account_roles WHERE account_id = :accountId")
            .param("accountId", accountId.value)
            .update()

        val now = OffsetDateTime.now()
        roleIds.forEach { roleId ->
            jdbcClient
                .sql(
                    """
                    INSERT INTO account_roles
                        (account_role_id, account_id, role_id, created_by, updated_by, created_at, updated_at)
                    VALUES
                        (:accountRoleId, :accountId, :roleId, :createdBy, :updatedBy, :createdAt, :updatedAt)
                    """.trimIndent(),
                ).param("accountRoleId", UUID.randomUUID())
                .param("accountId", accountId.value)
                .param("roleId", roleId)
                .param("createdBy", operatorId)
                .param("updatedBy", operatorId)
                .param("createdAt", now)
                .param("updatedAt", now)
                .update()
        }

        // 2. accounts.version インクリメント（楽観ロック: WHERE version = prevVersion）
        return jdbcClient
            .sql(
                """
                UPDATE accounts
                SET email        = :email,
                    name         = :name,
                    status       = :status,
                    suspended_at = :suspendedAt,
                    version      = :version,
                    updated_by   = :updatedBy,
                    updated_at   = :updatedAt
                WHERE account_id = :accountId
                  AND version    = :prevVersion
                """.trimIndent(),
            ).param("email", account.email)
            .param("name", account.name)
            .param("status", account.status.toDbValue())
            .param("suspendedAt", account.suspendedAt)
            .param("version", account.version)
            .param("updatedBy", account.updatedBy)
            .param("updatedAt", account.updatedAt)
            .param("accountId", account.accountId.value)
            .param("prevVersion", account.version - 1)
            .update()
    }
}
