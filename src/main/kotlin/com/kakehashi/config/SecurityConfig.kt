package com.kakehashi.config

import com.kakehashi.domain.account.JwtTokenIssuer
import com.kakehashi.infrastructure.account.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/**
 * Spring Security 設定（APP-ADR-0014）
 *
 * 根拠: docs/design/api/account-role.md（認証・認可節）
 *
 * - `POST /api/auth/google/callback`（UC-A1）は未認証で許可する（Google SSO コールバック受信のため）
 * - それ以外のエンドポイントは [JwtAuthenticationFilter] による自前JWT検証を通過させる
 * - セッションを使用しないステートレス API のため CSRF 保護・セッション管理は無効化する
 * - ロールベースの認可判定（`@PreAuthorize` 等）は exec-plan 0007 のスコープ
 *
 * `config` 層は全レイヤーに依存可能（DI 配線のため、docs/architecture/package-structure.md）。
 * [JwtAuthenticationFilter] は `@Component` を付与せず本クラスから明示的にインスタンス化する
 * （`@Component` にすると `@WebMvcTest` の型ベーススキャンで意図せず取り込まれるおそれがあるため）。
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtTokenIssuer: JwtTokenIssuer,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/api/auth/google/callback")
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }.addFilterBefore(
                JwtAuthenticationFilter(jwtTokenIssuer),
                UsernamePasswordAuthenticationFilter::class.java,
            )
        return http.build()
    }
}
