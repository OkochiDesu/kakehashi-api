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
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

/**
 * AccountRepositoryImpl 統合テスト
 *
 * - Testcontainers JDBC URL（jdbc:tc:postgresql:...）で PostgreSQL を自動起動し
 *   Flyway マイグレーション（V1, V2）を実行して検証する
 * - JdbcClient を使った実際の SQL が正しく動くことを保証する
 * - 楽観ロックの version カラム動作もここで検証する
 *
 * datasource は application-integration-test.properties で設定する。
 * @SpringBootTest + @ServiceConnection / @DynamicPropertySource は Spring Boot 4.x
 * で動作不安定なため、ContainerDatabaseDriver を使う方式を採用。
 * 詳細: docs/troubleshooting/testcontainers-jvmstatic-kotlin.md、APP-ADR-0005
 */
@SpringBootTest
@ActiveProfiles("integration-test")
@Transactional
class AccountRepositoryImplIntegrationTest {
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
        assertNull(repository.findById(AccountId("ZZ9999")))
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

        val withWrongVersion = account.copy(version = 1)
        val rows = repository.update(withWrongVersion)

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
