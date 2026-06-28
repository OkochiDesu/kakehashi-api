package com.kakehashi.infrastructure.account

import com.kakehashi.domain.account.Account
import com.kakehashi.domain.account.AccountId
import com.kakehashi.domain.account.AccountStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.OffsetDateTime
import java.util.UUID

/**
 * AccountRepositoryImpl 統合テスト
 *
 * - @ServiceConnection で PostgreSQLContainer を起動し DataSource・Flyway・MyBatis を自動設定する
 * - @ActiveProfiles("integration-test") で devcontainer の db:5432 への接続試行を防ぐ
 * - Flyway マイグレーション（V1, V2）は @ServiceConnection 経由で自動実行される
 * - JdbcClient を使った実際の SQL が正しく動くことを保証する
 * - 楽観ロックの version カラム動作もここで検証する
 *
 * 詳細: docs/troubleshooting/testcontainers-jvmstatic-kotlin.md、APP-ADR-0013（APP-ADR-0012 を Supersede）
 * ローカル実行には Docker socket へのアクセス権限が必要（devcontainer では CI で確認すること）
 *
 * ★★全体観点★★
 * 実際の PostgreSQL 16 コンテナを使い、MyBatis SQL・Flyway マイグレーション・楽観ロックが
 * 本番相当の環境で正しく動作することを保証する。
 * モックでは検出できない SQL 不整合・DDL 齟齬・トランザクション競合を排除するための統合テスト。
 *
 * 《観　点》Flyway DDL が正常に適用されていることの確認
 * 《テスト》Flyway マイグレーション（V1・V2）が正常に完了し accounts テーブルが存在する
 *
 * 《観　点》INSERT → SELECT の往復・各検索パスが SQL レベルで動作することの確認
 * 《テスト》正常系： save したアカウントを findById で取得できる
 * 《テスト》正常系： findByGoogleSubHash でアカウントを取得できる
 * 《テスト》正常系： 存在しない ID で findById は null を返す
 *
 * 《観　点》UPDATE の楽観ロック version インクリメントと監査カラム書き込みの確認
 * 《テスト》正常系： update で version がインクリメントされ updatedBy が反映される
 * 《テスト》異常系： version 不一致の update は 0件を返す（楽観ロック）
 *
 * 《観　点》ロール付与の DELETE/INSERT が正しく動作し version が更新されることの確認
 * 《テスト》正常系： assignRolesAndBumpVersion でロールが付与され version がインクリメントされる
 * 《テスト》正常系： assignRolesAndBumpVersion でロールを全剥奪できる
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("integration-test")
@Transactional
class AccountRepositoryImplIntegrationTest {
    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        @Suppress("DEPRECATION")
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
    }

    @Autowired
    lateinit var repository: AccountRepositoryImpl

    private fun buildAccount(
        accountId: String = "AZ0001",
        googleSubHash: String = "hash_$accountId",
        status: AccountStatus = AccountStatus.ACTIVE,
        version: Int = 0,
    ): Account {
        val now = OffsetDateTime.now()
        return Account(
            accountId = AccountId(accountId),
            googleSubHash = googleSubHash,
            email = "$accountId@example.com",
            name = "テストユーザー $accountId",
            status = status,
            suspendedAt = null,
            version = version,
            createdBy = accountId,
            updatedBy = accountId,
            createdAt = now,
            updatedAt = now,
        )
    }

    @Test
    fun `Flyway マイグレーション（V1・V2）が正常に完了し accounts テーブルが存在する`() {
        val seq = repository.nextAccountIdSequence()
        assertTrue(seq > 0, "accounts_account_id_seq が存在しシーケンスが取得できること")
    }

    @Test
    fun `正常系： save したアカウントを findById で取得できる`() {
        val account = buildAccount("AZ0001")
        repository.save(account)

        val found = repository.findById(AccountId("AZ0001"))

        assertNotNull(found)
        assertEquals("AZ0001", found!!.accountId.value)
        assertEquals("hash_AZ0001", found.googleSubHash)
        assertEquals(AccountStatus.ACTIVE, found.status)
        assertEquals(0, found.version)
    }

    @Test
    fun `正常系： 存在しない ID で findById は null を返す`() {
        assertNull(repository.findById(AccountId("AZ9999")))
    }

    @Test
    fun `正常系： findByGoogleSubHash でアカウントを取得できる`() {
        val account = buildAccount("AZ0002", googleSubHash = "unique_hash_0002")
        repository.save(account)

        val found = repository.findByGoogleSubHash("unique_hash_0002")

        assertNotNull(found)
        assertEquals("AZ0002", found!!.accountId.value)
    }

    @Test
    fun `正常系： update で version がインクリメントされ updatedBy が反映される`() {
        val account = buildAccount("AZ0003", version = 0)
        repository.save(account)

        val updated =
            account.copy(
                version = 1,
                name = "更新後の名前",
                updatedBy = "OPERATOR",
                updatedAt = OffsetDateTime.now(),
            )
        val rows = repository.update(updated)

        assertEquals(1, rows, "1行更新されること")
        val found = repository.findById(AccountId("AZ0003"))!!
        assertEquals(1, found.version)
        assertEquals("更新後の名前", found.name)
        assertEquals("OPERATOR", found.updatedBy)
    }

    @Test
    fun `異常系： version 不一致の update は 0件を返す（楽観ロック）`() {
        val account = buildAccount("AZ0004", version = 0)
        repository.save(account)

        // DB を version=1 に進める（別ユーザーが先に更新した状態を再現）
        val firstUpdate = account.copy(version = 1, name = "先行更新", updatedAt = OffsetDateTime.now())
        assertEquals(1, repository.update(firstUpdate), "前提: 1件更新されること")

        // stale な version=1 で再度更新を試みる → prevVersion=0 だが DB は 1 → 0件更新
        val staleUpdate = account.copy(version = 1, name = "古い更新", updatedAt = OffsetDateTime.now())
        val rows = repository.update(staleUpdate)

        assertEquals(0, rows, "version 不一致なら 0件更新")
    }

    @Test
    fun `正常系： assignRolesAndBumpVersion でロールが付与され version がインクリメントされる`() {
        val account = buildAccount("AZ0005", version = 0)
        repository.save(account)

        val roleId = UUID.fromString("01970000-0000-7000-8000-000000000001")
        val updated = account.copy(version = 1, updatedBy = "OPERATOR", updatedAt = OffsetDateTime.now())
        val rows =
            repository.assignRolesAndBumpVersion(
                accountId = AccountId("AZ0005"),
                roleIds = listOf(roleId),
                account = updated,
                operatorId = "OPERATOR",
            )

        assertEquals(1, rows, "1行更新されること")
        val roleIds = repository.findRoleIdsByAccountId(AccountId("AZ0005"))
        assertEquals(setOf(roleId), roleIds)
        assertEquals(1, repository.findById(AccountId("AZ0005"))!!.version)
    }

    @Test
    fun `正常系： assignRolesAndBumpVersion でロールを全剥奪できる`() {
        val account = buildAccount("AZ0006", version = 0)
        repository.save(account)

        val updated = account.copy(version = 1, updatedBy = "OPERATOR", updatedAt = OffsetDateTime.now())
        repository.assignRolesAndBumpVersion(
            accountId = AccountId("AZ0006"),
            roleIds = emptyList(),
            account = updated,
            operatorId = "OPERATOR",
        )

        assertTrue(repository.findRoleIdsByAccountId(AccountId("AZ0006")).isEmpty())
    }
}
