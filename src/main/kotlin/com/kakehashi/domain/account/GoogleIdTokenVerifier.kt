package com.kakehashi.domain.account

/**
 * Google ID トークン検証ポート（ドメイン層インターフェース）
 *
 * 根拠: docs/design/api/account-role.md（UC-A1）、APP-ADR-0014
 * 実装は infrastructure 層（Google JWKS を用いた署名検証、[NimbusJwtDecoder]）。
 * 本インターフェース自体は Spring / 特定ライブラリの型を持ち込まない。
 */
interface GoogleIdTokenVerifier {
    /**
     * Google ID トークンを検証し、検証済みの Google アカウント情報を返す。
     *
     * @param idToken Google が発行した ID トークン（JWT フォーマット済みであることは呼び出し側で保証する）
     * @return 検証済みの Google アカウント情報（sub のハッシュ・email・name）
     * @throws GoogleIdTokenVerificationFailedException 署名・iss/aud/有効期限の検証に失敗した場合
     */
    fun verify(idToken: String): GoogleIdentity
}

/**
 * 検証済みの Google アカウント情報
 *
 * @property googleSubHash Google sub クレームのハッシュ値（平文の sub はサーバー側で保持しない）
 * @property email Google アカウントのメールアドレス
 * @property name Google アカウントの表示名
 */
data class GoogleIdentity(
    val googleSubHash: String,
    val email: String,
    val name: String,
)

/**
 * Google ID トークンの検証に失敗したことを表すドメイン層例外
 *
 * usecase 層（[com.kakehashi.usecase.account.GoogleSsoCallbackUseCase]）でキャッチされ、
 * HTTP 応答用の usecase 層例外（401 Unauthorized）に変換される。
 */
class GoogleIdTokenVerificationFailedException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
