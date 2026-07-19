package com.kakehashi.infrastructure.account

import com.kakehashi.domain.account.Account
import com.kakehashi.domain.account.AccountId
import com.kakehashi.domain.account.AccountRepository
import com.kakehashi.domain.account.AccountStatus
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
 * APP-ADR-0016: MyBatis（AccountMapper）に統一する。MyBatis がリフレクションで直接触れる対象は
 *   中間 DTO（AccountRow）に限定し、Account（エンティティ本体、private constructor）には触れない。
 *   このクラスが「境界防波堤」として AccountRow ↔ Account の詰め替えを一手に引き受ける。
 */
@Repository
class AccountRepositoryImpl(
    private val accountMapper: AccountMapper,
) : AccountRepository {
    /**
     * アカウントを ID で取得する。
     *
     * 設計書No：UC-A3/A4/A6/A7
     * ADRNo：APP-ADR-0005, APP-ADR-0016
     */
    override fun findById(accountId: AccountId): Account? = accountMapper.findAccountRowById(accountId.value)?.toEntity()

    /**
     * Google sub ハッシュでアカウントを取得する（ログイン照合用）。
     *
     * 設計書No：UC-A1
     * ADRNo：APP-ADR-0005, APP-ADR-0016
     */
    override fun findByGoogleSubHash(googleSubHash: String): Account? =
        accountMapper.findAccountRowByGoogleSubHash(googleSubHash)?.toEntity()

    /**
     * アカウントを新規保存する（仮登録 UC-A1）。
     *
     * 設計書No：UC-A1
     * ADRNo：APP-ADR-0005
     */
    @Transactional
    override fun save(account: Account) {
        accountMapper.insertAccountRow(account.toRow())
    }

    /**
     * APP-ADR-0005: WHERE version = :prevVersion で楽観ロックを実装する
     * 更新件数 0 = version 不一致（呼び出し元で 409 Conflict に変換）
     */
    @Transactional
    override fun update(account: Account): Int = accountMapper.updateAccountRow(account.toRow(), account.version - 1)

    /**
     * PostgreSQL シーケンスから次の account_id 連番を取得する。
     *
     * 設計書No：UC-A1
     * ADRNo：APP-ADR-0005
     */
    override fun nextAccountIdSequence(): Long = accountMapper.nextAccountIdSequence()

    /**
     * 対象アカウントが保持する role_id 一覧を取得する。
     *
     * 設計書No：UC-A6
     * ADRNo：APP-ADR-0005
     */
    override fun findRoleIdsByAccountId(accountId: AccountId): Set<UUID> =
        accountMapper.findRoleIdsByAccountId(accountId.value).map { UUID.fromString(it) }.toSet()

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
        accountMapper.deleteAccountRoles(accountId.value)

        if (roleIds.isNotEmpty()) {
            val now = OffsetDateTime.now()
            val rows =
                roleIds.map { roleId ->
                    AccountRoleInsertRow(
                        accountRoleId = UUID.randomUUID().toString(),
                        accountId = accountId.value,
                        roleId = roleId.toString(),
                        createdBy = operatorId,
                        updatedBy = operatorId,
                        createdAt = now,
                        updatedAt = now,
                    )
                }
            accountMapper.insertAccountRoles(rows)
        }

        // 2. accounts.version インクリメント（楽観ロック: WHERE version = prevVersion）
        return accountMapper.updateAccountRow(account.toRow(), account.version - 1)
    }

    private fun AccountRow.toEntity(): Account =
        Account.reconstruct(
            accountId = AccountId(accountId),
            googleSubHash = googleSubHash,
            email = email,
            name = name,
            status = AccountStatus.fromDbValue(status),
            suspendedAt = suspendedAt,
            version = version,
            createdBy = createdBy,
            updatedBy = updatedBy,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun Account.toRow(): AccountRow =
        AccountRow(
            accountId = accountId.value,
            googleSubHash = googleSubHash,
            email = email,
            name = name,
            status = status.toDbValue(),
            suspendedAt = suspendedAt,
            version = version,
            createdBy = createdBy,
            updatedBy = updatedBy,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
