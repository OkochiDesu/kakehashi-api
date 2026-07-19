package com.kakehashi.infrastructure.account

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.time.OffsetDateTime

/**
 * アカウント MyBatis Mapper（Query 系・Command 系 DTO マッピング）
 *
 * Query 系は MyBatis Mapper でドメイン層をバイパスし、JOIN 結果を DTO に直接マッピングする。
 * Command 系（単一集約・全フィールド・ID セントリックな読み書き）はリフレクション対象を中間 DTO
 * （`AccountRow`）に限定し、エンティティへの変換は `AccountRepositoryImpl` が `reconstruct()` で
 * 手書きで行う（このインターフェース自体は `Account` に触れない）。
 * SQL は src/main/resources/mapper/account/AccountMapper.xml に記述する。
 *
 * 関連: docs/architecture/package-structure.md（infrastructure 層の責務）・APP-ADR-0008（Query系の設計原則）・
 * APP-ADR-0016（Repository実装のMyBatis統一・中間DTO限定方針）
 */
@Mapper
interface AccountMapper {
    /**
     * アカウント一覧・検索（UC-A5）
     * 動的条件（name 部分一致 / status 複数 / roleCode JOIN）は XML の <if> で制御
     */
    fun searchAccounts(
        @Param("name") name: String?,
        @Param("statuses") statuses: List<String>,
        @Param("roleCode") roleCode: String?,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int,
    ): List<AccountSummaryRow>

    /**
     * 検索条件に一致するアカウント総件数
     */
    fun countAccounts(
        @Param("name") name: String?,
        @Param("statuses") statuses: List<String>,
        @Param("roleCode") roleCode: String?,
    ): Long

    /**
     * アカウント詳細取得（account_roles / roles を JOIN）
     */
    fun findAccountDetailById(
        @Param("accountId") accountId: String,
    ): AccountDetailRow?

    /**
     * アカウントを ID で取得する（Command 系・単一集約読み取り、APP-ADR-0016）
     * @param accountId 取得対象のアカウントID
     * @return AccountRow、存在しない場合は null
     */
    fun findAccountRowById(
        @Param("accountId") accountId: String,
    ): AccountRow?

    /**
     * Google sub ハッシュでアカウントを取得する（Command 系・ログイン照合用）
     * @param googleSubHash Google sub クレームの SHA-256 ハッシュ値
     * @return AccountRow、存在しない場合は null
     */
    fun findAccountRowByGoogleSubHash(
        @Param("googleSubHash") googleSubHash: String,
    ): AccountRow?

    /**
     * アカウントを新規挿入する（仮登録 UC-A1 / UC-A2）
     * @param row 挿入する AccountRow
     */
    fun insertAccountRow(
        @Param("row") row: AccountRow,
    )

    /**
     * アカウントを更新する（楽観ロック: WHERE version = prevVersion）
     * @param row 更新後の AccountRow（version インクリメント済み）
     * @param prevVersion 更新前提の version（不一致なら 0 件更新）
     * @return 更新件数（0 の場合は version 不一致 → 409 Conflict）
     */
    fun updateAccountRow(
        @Param("row") row: AccountRow,
        @Param("prevVersion") prevVersion: Int,
    ): Int

    /**
     * PostgreSQL シーケンスから次の account_id 連番を取得する
     * @return nextval('accounts_account_id_seq')
     */
    fun nextAccountIdSequence(): Long

    /**
     * 対象アカウントが保持する role_id 一覧を取得する
     * @param accountId 取得対象のアカウントID
     * @return role_id（文字列表現）のリスト（ロールなしの場合は空リスト）
     */
    fun findRoleIdsByAccountId(
        @Param("accountId") accountId: String,
    ): List<String>

    /**
     * 対象アカウントの account_roles を全削除する（全置換の前段）
     * @param accountId 対象アカウントID
     */
    fun deleteAccountRoles(
        @Param("accountId") accountId: String,
    )

    /**
     * account_roles へロールを一括挿入する
     * @param rows 挿入する AccountRoleInsertRow のリスト（空リストの場合は呼び出さないこと）
     */
    fun insertAccountRoles(
        @Param("rows") rows: List<AccountRoleInsertRow>,
    )
}

/**
 * アカウント Command 系 中間 DTO（APP-ADR-0016）
 *
 * MyBatis がリフレクションで直接マッピングする対象はこの DTO に限定し、
 * `Account`（`private constructor` を持つエンティティ本体）には一度も触れない。
 * val プロパティのみを持つ不変な data class とする。
 *
 * @property accountId アカウントID
 * @property googleSubHash Google sub クレームの SHA-256 ハッシュ値
 * @property email メールアドレス
 * @property name 表示名
 * @property status アカウントステータス（DB 上の文字列表現）
 * @property suspendedAt 停止日時（未停止の場合 null）
 * @property version 楽観ロック用バージョン
 * @property createdBy 作成者の accountId
 * @property updatedBy 更新者の accountId
 * @property createdAt 作成日時
 * @property updatedAt 更新日時
 */
data class AccountRow(
    val accountId: String,
    val googleSubHash: String,
    val email: String,
    val name: String,
    val status: String,
    val suspendedAt: OffsetDateTime?,
    val version: Int,
    val createdBy: String,
    val updatedBy: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

/**
 * account_roles 一括挿入用 DTO（UC-A6）
 *
 * @property accountRoleId account_roles の主キー（UUID の文字列表現、呼び出し元で採番）
 * @property accountId 対象アカウントID
 * @property roleId 付与するロールID（UUID の文字列表現）
 * @property createdBy 作成者（操作者）の accountId
 * @property updatedBy 更新者（操作者）の accountId
 * @property createdAt 作成日時
 * @property updatedAt 更新日時
 */
data class AccountRoleInsertRow(
    val accountRoleId: String,
    val accountId: String,
    val roleId: String,
    val createdBy: String,
    val updatedBy: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

/**
 * アカウント一覧用 DTO
 *
 * @property accountId アカウントID
 * @property name 表示名
 * @property status アカウントステータス（DB 上の文字列表現）
 */
data class AccountSummaryRow(
    val accountId: String,
    val name: String,
    val status: String,
)

/**
 * アカウント詳細用 DTO
 *
 * @property accountId アカウントID
 * @property name 表示名
 * @property email メールアドレス
 * @property status アカウントステータス（DB 上の文字列表現）
 * @property suspendedAt 停止日時（ISO 8601 文字列、未停止の場合 null）
 * @property version 楽観ロック用バージョン
 * @property createdAt 作成日時（ISO 8601 文字列）
 * @property updatedAt 更新日時（ISO 8601 文字列）
 * @property updatedBy 更新者の accountId
 * @property roles 付与されているロール一覧
 */
data class AccountDetailRow(
    val accountId: String,
    val name: String,
    val email: String,
    val status: String,
    val suspendedAt: String?,
    val version: Int,
    val createdAt: String,
    val updatedAt: String,
    val updatedBy: String,
    val roles: List<RoleRow>,
)

/**
 * ロール情報 DTO
 *
 * @property roleId ロールID
 * @property code 権限コード（RoleCode の DB 上の文字列表現）
 * @property name ロール名
 */
data class RoleRow(
    val roleId: String,
    val code: String,
    val name: String,
)
