package com.kakehashi.infrastructure.account

import com.kakehashi.domain.account.GoogleIdTokenVerificationFailedException
import com.kakehashi.domain.account.GoogleIdTokenVerifier
import com.kakehashi.domain.account.GoogleIdentity
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component
import java.security.MessageDigest

/**
 * Google ID トークン検証の実装（APP-ADR-0014）
 *
 * 根拠: docs/design/api/account-role.md（UC-A1）
 *
 * Spring Security OAuth2 Resource Server が提供する [NimbusJwtDecoder] を Google の JWKS URI
 * （`https://www.googleapis.com/oauth2/v3/certs`）で構成し、署名検証に加えて標準クレーム
 * （iss / aud / exp）も検証する。平文の Google sub はサーバー側で保持せず SHA-256 ハッシュのみを返す。
 * `email_verified` クレームが true であることも検証する（false のまま `email` を信頼すると、
 * メール未確認の Google アカウントで `allowed-domains` によるドメイン制限をすり抜けられるため）。
 *
 * @param jwksUri Google の JWKS 配布エンドポイント（`app.auth.google.jwks-uri`）
 * @param issuer 期待する iss クレーム値（`app.auth.google.issuer`）
 * @param clientId 期待する aud クレーム値（Google OAuth Client ID、`app.auth.google.client-id`）
 */
@Component
class GoogleIdTokenVerifierImpl(
    @Value("\${app.auth.google.jwks-uri}") jwksUri: String,
    @Value("\${app.auth.google.issuer}") issuer: String,
    @Value("\${app.auth.google.client-id}") private val clientId: String,
) : GoogleIdTokenVerifier {
    private val jwtDecoder: NimbusJwtDecoder =
        NimbusJwtDecoder.withJwkSetUri(jwksUri).build().apply {
            val audienceValidator =
                OAuth2TokenValidator<Jwt> { jwt ->
                    if (jwt.audience.contains(clientId)) {
                        OAuth2TokenValidatorResult.success()
                    } else {
                        OAuth2TokenValidatorResult.failure(
                            OAuth2Error("invalid_token", "aud クレームが Client ID と一致しません", null),
                        )
                    }
                }
            setJwtValidator(
                DelegatingOAuth2TokenValidator(
                    JwtValidators.createDefaultWithIssuer(issuer),
                    audienceValidator,
                ),
            )
        }

    override fun verify(idToken: String): GoogleIdentity {
        val jwt =
            try {
                jwtDecoder.decode(idToken)
            } catch (e: JwtException) {
                throw GoogleIdTokenVerificationFailedException("Google ID トークンの検証に失敗しました", e)
            }

        return extractIdentity(jwt)
    }

    /**
     * デコード済みの [Jwt] から検証済みの Google アカウント情報を組み立てる。
     *
     * ネットワーク疎通を要する署名検証（[jwtDecoder]）から切り離すことで、[Jwt] を直接構築する
     * 単体テストで検証できるようにするため `internal` としている。
     *
     * @param jwt 署名検証済みの Google ID トークン
     * @return 検証済みの Google アカウント情報（sub のハッシュ・email・name）
     * @throws GoogleIdTokenVerificationFailedException email_verified クレームが true でない場合、
     *   sub クレームが含まれない場合、または email クレームが含まれない場合
     */
    internal fun extractIdentity(jwt: Jwt): GoogleIdentity {
        val emailVerified = jwt.getClaimAsBoolean("email_verified") ?: false
        if (!emailVerified) {
            throw GoogleIdTokenVerificationFailedException(
                "Google ID トークンの email_verified が true ではありません（未確認のメールアドレスは信頼できません）",
            )
        }

        val sub =
            jwt.subject
                ?: throw GoogleIdTokenVerificationFailedException("Google ID トークンに sub クレームが含まれていません")
        val email =
            jwt.getClaimAsString("email")
                ?: throw GoogleIdTokenVerificationFailedException("Google ID トークンに email クレームが含まれていません")
        val name = jwt.getClaimAsString("name") ?: email

        return GoogleIdentity(
            googleSubHash = hashSub(sub),
            email = email,
            name = name,
        )
    }

    /** Google sub クレームを SHA-256 でハッシュ化する（平文の sub はサーバー側で保持しない） */
    private fun hashSub(sub: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(sub.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }
}
