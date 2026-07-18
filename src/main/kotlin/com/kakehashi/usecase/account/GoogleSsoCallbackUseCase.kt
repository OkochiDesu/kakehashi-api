package com.kakehashi.usecase.account

import com.kakehashi.domain.account.Account
import com.kakehashi.domain.account.AccountId
import com.kakehashi.domain.account.AccountRepository
import com.kakehashi.domain.account.AccountStatus
import com.kakehashi.domain.account.GoogleIdTokenVerificationFailedException
import com.kakehashi.domain.account.GoogleIdTokenVerifier
import com.kakehashi.domain.account.JwtTokenIssuer
import com.kakehashi.usecase.account.exception.DomainNotAllowedException
import com.kakehashi.usecase.account.exception.ForbiddenOperationException
import com.kakehashi.usecase.account.exception.GoogleIdTokenVerificationException
import com.kakehashi.usecase.account.exception.InvalidIdTokenFormatException
import java.time.OffsetDateTime

/**
 * UC-A1: Google SSO ログイン（仮登録・自動プロビジョニング・自前JWT発行）UseCase
 *
 * 根拠: docs/design/api/account-role.md（UC-A1）、APP-ADR-0014
 *
 * 認証フローの3段構成を1クラスで統合する:
 * 1. Google ID トークン検証（[googleIdTokenVerifier]、Google JWKS による署名・iss/aud/有効期限検証）
 * 2. JIT プロビジョニング（未登録の Google アカウントは PROVISIONAL で仮登録）
 * 3. 自前 JWT 発行（[jwtTokenIssuer]、accountId クレームを含む JWT を発行）
 *
 * suspended / deactivated アカウント（[AccountStatus.canLogin] が false）はログインを拒否する
 * （JIT 再作成は行わず、自前 JWT も発行しない）。
 *
 * @property accountRepository アカウント永続化ポート
 * @property googleIdTokenVerifier Google ID トークン検証ポート
 * @property jwtTokenIssuer 自前 JWT 発行ポート
 * @property allowedGoogleDomains ログインを許可する Google アカウントのメールドメイン一覧
 *   （小文字・空セットの場合はドメイン制限なし。`app.auth.google.allowed-domains` プロパティ由来）
 */
class GoogleSsoCallbackUseCase(
    private val accountRepository: AccountRepository,
    private val googleIdTokenVerifier: GoogleIdTokenVerifier,
    private val jwtTokenIssuer: JwtTokenIssuer,
    private val allowedGoogleDomains: Set<String>,
) {
    data class Input(
        val idToken: String,
    )

    data class Output(
        val accountId: String,
        val status: AccountStatus,
        val accessToken: String,
        val redirectTo: String,
    )

    /**
     * Google SSO コールバックを処理し、検証・JIT プロビジョニング・自前 JWT 発行を行う。
     *
     * 設計書No：UC-A1
     * ADRNo：APP-ADR-0014
     *
     * @param input Google の idToken を含む入力値
     * @return accountId・status・自前 JWT・リダイレクト先を含む出力値
     * @throws InvalidIdTokenFormatException idToken が JWT フォーマットとして不正な場合
     * @throws GoogleIdTokenVerificationException Google JWKS による署名・iss/aud/有効期限の検証に失敗した場合
     * @throws DomainNotAllowedException 許可ドメインが設定されており、かつメールアドレスのドメインが許可リストに含まれない場合
     * @throws ForbiddenOperationException 対象アカウントの [AccountStatus.canLogin] が false の場合（suspended/deactivated）
     */
    fun execute(input: Input): Output {
        if (!isJwtFormat(input.idToken)) {
            throw InvalidIdTokenFormatException("idToken の形式が不正です（JWT形式である必要があります）")
        }

        val identity =
            try {
                googleIdTokenVerifier.verify(input.idToken)
            } catch (e: GoogleIdTokenVerificationFailedException) {
                throw GoogleIdTokenVerificationException(e.message ?: "Google IDトークンの検証に失敗しました")
            }

        val emailDomain = identity.email.substringAfterLast("@", missingDelimiterValue = "").lowercase()
        if (allowedGoogleDomains.isNotEmpty() && emailDomain !in allowedGoogleDomains) {
            throw DomainNotAllowedException("許可されていないドメインの Google アカウントです: $emailDomain")
        }

        val existing = accountRepository.findByGoogleSubHash(identity.googleSubHash)

        val account =
            if (existing == null) {
                // 初回ログイン: 仮登録（UC-A2 相当の内部処理）
                val seq = accountRepository.nextAccountIdSequence()
                val newAccountId = AccountId.fromSequence(seq)
                val now = OffsetDateTime.now()
                val newAccount =
                    Account(
                        accountId = newAccountId,
                        googleSubHash = identity.googleSubHash,
                        email = identity.email,
                        name = identity.name,
                        status = AccountStatus.PROVISIONAL,
                        suspendedAt = null,
                        version = 0,
                        createdBy = newAccountId.value,
                        updatedBy = newAccountId.value,
                        createdAt = now,
                        updatedAt = now,
                    )
                accountRepository.save(newAccount)
                newAccount
            } else {
                existing
            }

        // canTransitionTo() は状態「遷移」の可否判定であり、ここでは遷移させず現状態での
        // ログイン可否のみを判定するため canLogin() を用いる（UC-A1 の許可される元ステータス: provisional/active）。
        if (!account.status.canLogin()) {
            throw ForbiddenOperationException(
                "アカウント ${account.accountId.value} はログインできません（status: ${account.status}）",
            )
        }

        val redirectTo =
            when (account.status) {
                AccountStatus.PROVISIONAL -> "/registration"
                AccountStatus.ACTIVE -> "/mypage"
                AccountStatus.SUSPENDED, AccountStatus.DEACTIVATED ->
                    error("到達しないはずの分岐です（canLogin() チェック済み）: ${account.status}")
            }

        val accessToken = jwtTokenIssuer.issue(account.accountId)

        return Output(
            accountId = account.accountId.value,
            status = account.status,
            accessToken = accessToken,
            redirectTo = redirectTo,
        )
    }

    /**
     * トークンが JWS Compact Serialization の形式（header.payload.signature）に一致するかを返す。
     *
     * @param token 検証対象のトークン文字列
     * @return 形式が一致する場合 true
     */
    private fun isJwtFormat(token: String): Boolean = JWT_FORMAT_REGEX.matches(token)

    companion object {
        // JWS Compact Serialization（header.payload.signature、base64url セグメント）の形式チェック
        private val JWT_FORMAT_REGEX = Regex("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$")
    }
}
