package com.kakehashi.domain.account

import java.util.UUID

/**
 * アカウントリポジトリ インターフェース（ポート）
 *
 * 根拠: docs/architecture/package-structure.md（リポジトリI/Fの責務）
 * APP-ADR-0008: Command 系は集約 → Repository → DB の流れ
 * 実装は infrastructure/account/AccountRepositoryImpl
 */
interface AccountRepository {
    /**
     * アカウントを ID で取得する
     * @param accountId 取得対象のアカウントID
     * @return Account、存在しない場合は null
     */
    fun findById(accountId: AccountId): Account?

    /**
     * Google sub ハッシュでアカウントを取得する（ログイン照合用）
     * @param googleSubHash Google sub クレームの SHA-256 ハッシュ値
     * @return Account、存在しない場合は null
     */
    fun findByGoogleSubHash(googleSubHash: String): Account?

    /**
     * アカウントを新規保存する（仮登録 UC-A1 / UC-A2）
     * @param account 保存するアカウント（status = provisional）
     */
    fun save(account: Account)

    /**
     * アカウントを更新する（楽観ロック: version 一致を確認）
     * @param account 更新後のアカウント（version インクリメント済み）
     * @return 更新件数（0 の場合は version 不一致 → 409 Conflict）
     */
    fun update(account: Account): Int

    /**
     * PostgreSQL シーケンスから次の account_id 連番を取得する
     * @return nextval('accounts_account_id_seq')
     */
    fun nextAccountIdSequence(): Long

    /**
     * 対象アカウントが保持する role_id 一覧を取得する
     * @param accountId 取得対象のアカウントID
     * @return role_id の Set（ロールなしの場合は空 Set）
     */
    fun findRoleIdsByAccountId(accountId: AccountId): Set<UUID>

    /**
     * account_roles 全置換と accounts.version インクリメントを1トランザクションで実行する（UC-A6: 修正4）
     *
     * replaceRoles と update を別々に呼ぶと、replaceRoles 成功後に update が失敗した場合に
     * account_roles のみ書き換わる中間不整合が発生するため、1メソッドにまとめる。
     * @Transactional は実装クラス（AccountRepositoryImpl）に付与する（domain 層に Spring を持ち込まない）
     *
     * @param accountId 対象アカウントID
     * @param roleIds 付与するロールID一覧（空リスト = 全剥奪）
     * @param account バージョンインクリメント済みの Account（version = oldVersion + 1）
     * @param operatorId 操作者の accountId
     * @return accounts の更新件数（0 なら楽観ロック競合）
     */
    fun assignRolesAndBumpVersion(
        accountId: AccountId,
        roleIds: List<UUID>,
        account: Account,
        operatorId: String,
    ): Int
}
