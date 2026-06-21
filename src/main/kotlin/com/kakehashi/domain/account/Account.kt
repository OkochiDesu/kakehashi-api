package com.kakehashi.domain.account

import java.time.OffsetDateTime

/**
 * アカウント集約（ドメインエンティティ）
 *
 * 根拠: docs/requirements/data-models.md 1章
 * - フレームワーク非依存（Spring / MyBatis のアノテーションを持ち込まない）
 * - 状態遷移ルールは AccountStatus Enum に委譲する
 * - APP-ADR-0005: version による楽観ロック
 */
data class Account(
    val accountId: AccountId,
    val googleSubHash: String,
    val email: String,
    val name: String,
    val status: AccountStatus,
    val suspendedAt: OffsetDateTime?,
    val version: Int,
    val createdBy: String,
    val updatedBy: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
) {
    /**
     * 本登録申込み（UC-A3）: PROVISIONAL → ACTIVE へ遷移する
     * @param updatedBy 操作者の accountId
     * @return 遷移後の Account
     * @throws IllegalStateException 遷移不可の場合
     */
    fun register(updatedBy: String): Account {
        check(status.canTransitionTo(AccountStatus.ACTIVE)) {
            "Cannot transition from $status to ACTIVE"
        }
        return copy(
            status = AccountStatus.ACTIVE,
            version = version + 1,
            updatedBy = updatedBy,
            updatedAt = OffsetDateTime.now(),
        )
    }

    /**
     * アカウント情報編集（UC-A4）: 表示名を更新する
     * @param name 新しい表示名（空文字列不可）
     * @param updatedBy 操作者の accountId
     * @return 更新後の Account
     */
    fun editName(
        name: String,
        updatedBy: String,
    ): Account {
        require(name.isNotBlank()) { "name must not be blank" }
        return copy(
            name = name,
            version = version + 1,
            updatedBy = updatedBy,
            updatedAt = OffsetDateTime.now(),
        )
    }

    /**
     * アカウント停止（UC-A7）: ACTIVE → SUSPENDED へ遷移する
     * @param updatedBy 操作者の accountId
     * @return 遷移後の Account
     * @throws IllegalStateException 遷移不可の場合
     */
    fun suspend(updatedBy: String): Account {
        check(status.canTransitionTo(AccountStatus.SUSPENDED)) {
            "Cannot transition from $status to SUSPENDED"
        }
        return copy(
            status = AccountStatus.SUSPENDED,
            suspendedAt = OffsetDateTime.now(),
            version = version + 1,
            updatedBy = updatedBy,
            updatedAt = OffsetDateTime.now(),
        )
    }

    /**
     * アカウント停止解除（UC-A7）: SUSPENDED → ACTIVE へ遷移する
     * @param updatedBy 操作者の accountId
     * @return 遷移後の Account
     * @throws IllegalStateException 遷移不可の場合
     */
    fun unsuspend(updatedBy: String): Account {
        check(status.canTransitionTo(AccountStatus.ACTIVE)) {
            "Cannot transition from $status to ACTIVE"
        }
        return copy(
            status = AccountStatus.ACTIVE,
            suspendedAt = null,
            version = version + 1,
            updatedBy = updatedBy,
            updatedAt = OffsetDateTime.now(),
        )
    }
}
