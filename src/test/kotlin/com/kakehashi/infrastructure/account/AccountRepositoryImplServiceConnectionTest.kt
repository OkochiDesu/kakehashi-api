package com.kakehashi.infrastructure.account

import org.junit.jupiter.api.Assertions.assertNotNull
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

/**
 * @ServiceConnection パターン動作検証テスト
 *
 * APP-ADR-0012 の「今後の見直しポイント」に記載された
 * @ServiceConnection 安定動作確認のための検証用クラス。
 * 成功すれば AccountRepositoryImplIntegrationTest を本パターンに移行し
 * APP-ADR-0012 を Supersede する。
 *
 * 《観　点》@ServiceConnection で DataSource が正しく設定され Flyway と MyBatis が動作する
 * 《テスト》正常系： シーケンス取得で accounts テーブルへの接続が確認できる
 * 《テスト》正常系： save → findById の往復が正常に動作する
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("integration-test")
@Transactional
class AccountRepositoryImplServiceConnectionTest {
    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
    }

    @Autowired
    lateinit var repository: AccountRepositoryImpl

    @Test
    fun `正常系： @ServiceConnection で Flyway マイグレーションが完了し accounts テーブルへ接続できる`() {
        val seq = repository.nextAccountIdSequence()
        assertTrue(seq > 0, "シーケンスが取得できること")
    }

    @Test
    fun `正常系： @ServiceConnection で save → findById が正常に動作する`() {
        val now = java.time.OffsetDateTime.now()
        val account =
            com.kakehashi.domain.account.Account(
                accountId =
                    com.kakehashi.domain.account
                        .AccountId("AZ9901"),
                googleSubHash = "service_connection_test_hash",
                email = "AZ9901@example.com",
                name = "ServiceConnection検証ユーザー",
                status = com.kakehashi.domain.account.AccountStatus.ACTIVE,
                suspendedAt = null,
                version = 0,
                createdBy = "AZ9901",
                updatedBy = "AZ9901",
                createdAt = now,
                updatedAt = now,
            )
        repository.save(account)

        val found =
            repository.findById(
                com.kakehashi.domain.account
                    .AccountId("AZ9901"),
            )
        assertNotNull(found)
    }
}
