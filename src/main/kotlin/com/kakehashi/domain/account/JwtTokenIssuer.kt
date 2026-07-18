package com.kakehashi.domain.account

/**
 * 自前 JWT 発行・検証ポート（ドメイン層インターフェース）
 *
 * 根拠: docs/design/api/account-role.md（UC-A1）、APP-ADR-0014
 * 実装は infrastructure 層（jjwt を用いた署名・検証）。
 * 本インターフェース自体は Spring / jjwt 等の型を持ち込まない。
 */
interface JwtTokenIssuer {
    /**
     * accountId クレームを含む自前 JWT を発行する。
     *
     * @param accountId 発行対象のアカウントID
     * @return 署名済みの JWT 文字列
     */
    fun issue(accountId: AccountId): String

    /**
     * 自前 JWT を検証し、accountId クレームを取り出す。
     *
     * @param token `Authorization: Bearer` で受け取った JWT 文字列
     * @return 検証済みの accountId
     * @throws JwtVerificationFailedException 署名・有効期限の検証に失敗した場合、token が空文字列等の
     *   不正な形式の場合、または accountId クレームが不正な場合
     */
    fun verify(token: String): AccountId
}

/**
 * 自前 JWT の検証に失敗したことを表すドメイン層例外
 *
 * [com.kakehashi.infrastructure.account.JwtAuthenticationFilter] でキャッチされ、401 応答に変換される。
 */
class JwtVerificationFailedException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
