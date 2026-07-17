package com.kakehashi.config

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * 認証基盤（APP-ADR-0014）の設定値を起動時に検証する fail-fast ガード
 *
 * 根拠: code-reviewer 指摘（exec-plan 0006 差し戻し）。
 * `app.auth.jwt.secret` は環境変数 `JWT_SECRET` 未設定時に公知の開発用デフォルト値へ
 * サイレントにフォールバックする（`application.properties` 参照）。この鍵を知る者は任意の
 * `accountId` の JWT を偽造できるため、本番相当環境ではデフォルト値のまま・鍵長不足での
 * 起動を許してはならない。同様に `app.auth.google.allowed-domains` が空の場合、
 * ドメイン制限なしとして扱われ社外の任意の Google アカウントでログインが成立してしまう。
 *
 * `@Profile("!test & !integration-test")` を付与しているが、これは「プロファイル未指定なら無効」
 * という意味ではない点に注意（`!test & !integration-test` はプロファイル未指定時 true＝有効になる）。
 * 実際に既存テストで本ガードが発火しないのは以下の理由による:
 * - `@WebMvcTest` 等のスライステストは `@Component` を型スキャンで読み込まないため、
 *   そもそも `AuthStartupValidator` 自体がコンテキストに登録されない
 * - `AccountRepositoryImplIntegrationTest` 等は `@ActiveProfiles("integration-test")` を
 *   明示しており `!integration-test` により無効化される
 * `test` プロファイルは現状未使用だが将来の追加に備えて明示的に除外している。
 * **注意**: プロファイル未指定のフルコンテキスト `@SpringBootTest`（例: 将来再有効化される
 * `KakehashiApiApplicationTests`）を追加する場合、本ガードは有効化されるため、
 * `@ActiveProfiles("integration-test")` の指定または有効な `JWT_SECRET`/`GOOGLE_ALLOWED_DOMAINS`
 * の設定が必要になる（残課題は exec-plan 0006 参照）。
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
     *   32byte(256bit) 未満、または allowedDomains が空白の場合
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
        check(allowedDomains.isNotBlank()) {
            "app.auth.google.allowed-domains が未設定です。" +
                "本番相当環境では環境変数 GOOGLE_ALLOWED_DOMAINS に許可するドメインを設定してください"
        }
    }

    private companion object {
        // application.properties のデフォルト値と一致させること（drift 防止のため変更時は両方更新する）
        const val DEFAULT_JWT_SECRET = "dev-only-jwt-secret-please-override-in-production-min-32bytes"
        const val MIN_JWT_SECRET_BYTES = 32
    }
}
