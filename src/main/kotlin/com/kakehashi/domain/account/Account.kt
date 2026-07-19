package com.kakehashi.domain.account

import java.time.OffsetDateTime

/**
 * アカウント集約（ドメインエンティティ）
 *
 * 根拠: docs/requirements/data-models.md 1章
 * - フレームワーク非依存（Spring / MyBatis のアノテーションを持ち込まない）
 * - 状態遷移ルールは AccountStatus Enum に委譲する
 * - APP-ADR-0005: version による楽観ロック
 * - APP-ADR-0015: 通常 class として実装し、ID 基準の equals()/hashCode()・PII 安全な toString()・
 *   private constructor + companion object ファクトリ・private withChanges() を備える
 */
class Account private constructor(
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
    override fun equals(other: Any?): Boolean = other is Account && other.accountId == accountId

    override fun hashCode(): Int = accountId.hashCode()

    override fun toString(): String = "Account(accountId=$accountId, status=$status)"

    /**
     * 本登録申込み（UC-A3）: PROVISIONAL → ACTIVE へ遷移する
     * @param updatedBy 操作者の accountId
     * @return 遷移後の Account
     * @throws IllegalStateException 遷移不可の場合
     */
    fun register(updatedBy: String): Account {
        check(status == AccountStatus.PROVISIONAL) {
            "register() は PROVISIONAL のみ実行可能です（現在のステータス: $status）"
        }
        return withChanges(status = AccountStatus.ACTIVE, updatedBy = updatedBy)
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
        require(name.isNotBlank()) { "表示名は空文字列にできません" }
        return withChanges(name = name, updatedBy = updatedBy)
    }

    /**
     * アカウント停止（UC-A7）: ACTIVE → SUSPENDED へ遷移する
     * @param updatedBy 操作者の accountId
     * @return 遷移後の Account
     * @throws IllegalStateException 遷移不可の場合
     */
    fun suspend(updatedBy: String): Account {
        check(status.canTransitionTo(AccountStatus.SUSPENDED)) {
            "$status から SUSPENDED への遷移は許可されていません"
        }
        return withChanges(status = AccountStatus.SUSPENDED, suspendedAt = OffsetDateTime.now(), updatedBy = updatedBy)
    }

    /**
     * アカウント停止解除（UC-A7）: SUSPENDED → ACTIVE へ遷移する
     * @param updatedBy 操作者の accountId
     * @return 遷移後の Account
     * @throws IllegalStateException 遷移不可の場合
     */
    fun unsuspend(updatedBy: String): Account {
        check(status == AccountStatus.SUSPENDED) {
            "unsuspend() は SUSPENDED のみ実行可能です（現在のステータス: $status）"
        }
        return withChanges(status = AccountStatus.ACTIVE, suspendedAt = null, updatedBy = updatedBy)
    }

    /**
     * ロール付与・変更（UC-A6）に伴う version インクリメント。
     *
     * account_roles は Account のフィールドとして保持しないため、このメソッド自体は
     * ロール内容を変更しない（呼び出し元が Repository 経由で account_roles を全置換する）。
     * 集約としての version・updatedBy・updatedAt のみを更新する。
     *
     * @param updatedBy 操作者の accountId
     * @return version がインクリメントされた Account
     */
    fun assignRoles(updatedBy: String): Account = withChanges(updatedBy = updatedBy)

    private fun withChanges(
        status: AccountStatus = this.status,
        suspendedAt: OffsetDateTime? = this.suspendedAt,
        name: String = this.name,
        updatedBy: String = this.updatedBy,
    ): Account =
        Account(
            accountId = accountId,
            googleSubHash = googleSubHash,
            email = email,
            name = name,
            status = status,
            suspendedAt = suspendedAt,
            version = version + 1,
            createdBy = createdBy,
            updatedBy = updatedBy,
            createdAt = createdAt,
            updatedAt = OffsetDateTime.now(),
        )

    companion object {
        /**
         * DB 行など既存の永続化状態から Account を再構築する。
         * @param accountId アカウントID
         * @param googleSubHash Google sub のハッシュ値
         * @param email メールアドレス
         * @param name 表示名
         * @param status アカウントステータス
         * @param suspendedAt 停止日時（未停止の場合 null）
         * @param version 楽観ロック用バージョン
         * @param createdBy 作成者の accountId
         * @param updatedBy 更新者の accountId
         * @param createdAt 作成日時
         * @param updatedAt 更新日時
         * @return 再構築された Account
         */
        fun reconstruct(
            accountId: AccountId,
            googleSubHash: String,
            email: String,
            name: String,
            status: AccountStatus,
            suspendedAt: OffsetDateTime?,
            version: Int,
            createdBy: String,
            updatedBy: String,
            createdAt: OffsetDateTime,
            updatedAt: OffsetDateTime,
        ): Account =
            Account(
                accountId = accountId,
                googleSubHash = googleSubHash,
                email = email,
                name = name,
                status = status,
                suspendedAt = suspendedAt,
                version = version,
                createdBy = createdBy,
                updatedBy = updatedBy,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )

        /**
         * Google SSO 初回ログイン時に PROVISIONAL 状態で新規発行する（UC-A1 JIT プロビジョニング）。
         * @param accountId 採番済みの accountId
         * @param googleSubHash Google sub のハッシュ値
         * @param email メールアドレス
         * @param name 表示名
         * @return version=0・PROVISIONAL 状態の新規 Account
         */
        fun provision(
            accountId: AccountId,
            googleSubHash: String,
            email: String,
            name: String,
        ): Account {
            val now = OffsetDateTime.now()
            return Account(
                accountId = accountId,
                googleSubHash = googleSubHash,
                email = email,
                name = name,
                status = AccountStatus.PROVISIONAL,
                suspendedAt = null,
                version = 0,
                createdBy = accountId.value,
                updatedBy = accountId.value,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
