package com.kakehashi.usecase.account

import com.kakehashi.domain.account.Account
import com.kakehashi.domain.account.AccountId
import com.kakehashi.domain.account.AccountStatus
import java.time.OffsetDateTime

/** UseCase テストで共通利用するフィクスチャ */
object AccountTestFixtures {
    val now: OffsetDateTime = OffsetDateTime.parse("2026-01-01T00:00:00+09:00")

    fun buildAccount(
        accountId: String = "AZ0001",
        status: AccountStatus = AccountStatus.ACTIVE,
        version: Int = 0,
        suspendedAt: OffsetDateTime? = null,
    ): Account =
        Account(
            accountId = AccountId(accountId),
            googleSubHash = "hash_$accountId",
            email = "user@example.com",
            name = "テストユーザー",
            status = status,
            suspendedAt = suspendedAt,
            version = version,
            createdBy = accountId,
            updatedBy = accountId,
            createdAt = now,
            updatedAt = now,
        )
}
