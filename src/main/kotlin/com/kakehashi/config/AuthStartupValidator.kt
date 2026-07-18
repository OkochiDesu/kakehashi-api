package com.kakehashi.config

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * 認証基盤（APP-ADR-0014）の設定値を起動時に検証する fail-fast ガード
 *
 * `app.auth.jwt.secret`・`app.auth.google.allowed-domains` が本番相当環境で
 * デフォルト値・未設定のまま起動するのを防ぐ。検証の背景・経緯は exec-plan 0006 の
 * 意思決定ログ（2026-07-17）を参照。
 *
 * 注意: `@Profile("!test & !integration-test")` はプロファイル未指定時に有効になる
 * （Spring の標準セマンティクス）。スライステスト（`@WebMvcTest` 等）で本ガードが発火しないのは
 * `@Component` を型スキャンしないためであり、プロファイル条件によるものではない。
 * プロファイル未指定のフルコンテキスト `@SpringBootTest` を追加する場合は本ガードが有効化される点に注意。
 *
 * @param jwtSecret `app.auth.jwt.secret` プロパティの値
 * @param allowedDomains `app.auth.google.allowed-domains` プロパティの値（カンマ区切り、未加工）
 */
@Component
@Profile("!test & !integration-test")
class AuthStartupValidator(
    @Value("\${app.auth.jwt.secret}") private val jwtSecret: String,
    @Value("\${app.auth.google.allowed-domains:}") private val allowedDomains: String,
) {
    /**
     * 起動時に呼び出され、認証基盤の設定値が本番相当環境として妥当かを検証する。
     *
     * @throws IllegalStateException jwtSecret が開発用デフォルト値のまま、jwtSecret が
     *   32byte(256bit) 未満、または allowedDomains のパース結果（[AllowedDomainsParser.parse]）が
     *   空集合の場合
     */
    @PostConstruct
    fun validate() {
        check(jwtSecret != DEFAULT_JWT_SECRET) {
            "app.auth.jwt.secret に開発用デフォルト値が設定されたままです。" +
                "本番相当環境では環境変数 JWT_SECRET に32byte(256bit)以上のランダム値を設定してください"
        }
        check(jwtSecret.toByteArray(Charsets.UTF_8).size >= MIN_JWT_SECRET_BYTES) {
            "app.auth.jwt.secret の鍵長が ${MIN_JWT_SECRET_BYTES}byte(256bit) 未満です。" +
                "本番相当環境では環境変数 JWT_SECRET に十分な長さのランダム値を設定してください"
        }
        // isNotBlank() だけでは "," のようなカンマのみの値を通過させてしまい、
        // AccountUseCaseConfig 側のパース結果は空集合（ドメイン制限なし）になる不整合が起きるため、
        // 実際にパースした結果で判定する
        check(AllowedDomainsParser.parse(allowedDomains).isNotEmpty()) {
            "app.auth.google.allowed-domains に有効なドメインが1件も含まれていません。" +
                "本番相当環境では環境変数 GOOGLE_ALLOWED_DOMAINS に許可するドメインを設定してください"
        }
    }

    private companion object {
        // application.properties のデフォルト値と一致させること（drift 防止のため変更時は両方更新する）
        const val DEFAULT_JWT_SECRET = "dev-only-jwt-secret-please-override-in-production-min-32bytes"
        const val MIN_JWT_SECRET_BYTES = 32
    }
}
