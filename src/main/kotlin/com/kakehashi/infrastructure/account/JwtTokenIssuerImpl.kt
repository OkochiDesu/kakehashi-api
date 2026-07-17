package com.kakehashi.infrastructure.account

import com.kakehashi.domain.account.AccountId
import com.kakehashi.domain.account.JwtTokenIssuer
import com.kakehashi.domain.account.JwtVerificationFailedException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

/**
 * 自前 JWT 発行・検証の実装（APP-ADR-0014）
 *
 * 根拠: docs/design/api/account-role.md（UC-A1）
 *
 * 署名鍵管理方針（本実装時の決定。ADR-0014「影響」で実装時決定とされた事項）:
 * - HMAC 共有鍵（HS256）を採用する。検証者がバックエンド自身のみ（自前発行・自前検証であり、
 *   Google のように外部サービスへ公開鍵を配布する必要がない）ため、非対称鍵（RS256等）より
 *   鍵管理がシンプルな対称鍵で要件を満たせると判断した。
 * - 鍵は `app.auth.jwt.secret` プロパティ（環境変数 `JWT_SECRET`）で注入する。
 *   HS256 は最低 256bit（32byte）の鍵長を要求するため、本番環境では十分な長さのランダム値を
 *   必ず環境変数で上書きすること。`application.properties` のデフォルト値は開発・CI 用であり
 *   本番では使用しないこと。
 *
 * 有効期限方針（本実装時の決定）:
 * - 60分（3600秒）固定とする。リフレッシュトークンは ADR-0014 で「実装時に別途決定」と
 *   スコープ外にされており本実装では導入しない。有効期限切れ後は再度 Google SSO ログインが必要になる。
 *   運用上の不便が顕在化した場合はリフレッシュトークン導入を再検討する（ADR-0014 見直しポイント）。
 */
@Component
class JwtTokenIssuerImpl(
    @Value("\${app.auth.jwt.secret}") secret: String,
    @Value("\${app.auth.jwt.expiration-seconds:3600}") private val expirationSeconds: Long,
) : JwtTokenIssuer {
    private val signingKey: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8))

    override fun issue(accountId: AccountId): String {
        val now = Instant.now()
        return Jwts
            .builder()
            .subject(accountId.value)
            .claim(ACCOUNT_ID_CLAIM, accountId.value)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(expirationSeconds)))
            .signWith(signingKey)
            .compact()
    }

    override fun verify(token: String): AccountId {
        val claims =
            try {
                Jwts
                    .parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .payload
            } catch (e: JwtException) {
                throw JwtVerificationFailedException("JWTの検証に失敗しました", e)
            }

        val accountIdValue =
            claims.get(ACCOUNT_ID_CLAIM, String::class.java)
                ?: throw JwtVerificationFailedException("JWTにaccountIdクレームが含まれていません")

        return try {
            AccountId(accountIdValue)
        } catch (e: IllegalArgumentException) {
            throw JwtVerificationFailedException("JWTのaccountIdクレームの形式が不正です: $accountIdValue", e)
        }
    }

    companion object {
        private const val ACCOUNT_ID_CLAIM = "accountId"
    }
}
