package com.kakehashi.config

import com.kakehashi.domain.account.AccountRepository
import com.kakehashi.domain.account.GoogleIdTokenVerifier
import com.kakehashi.domain.account.JwtTokenIssuer
import com.kakehashi.infrastructure.account.AccountMapper
import com.kakehashi.usecase.account.AssignRolesUseCase
import com.kakehashi.usecase.account.EditAccountUseCase
import com.kakehashi.usecase.account.GetAccountQuery
import com.kakehashi.usecase.account.GoogleSsoCallbackUseCase
import com.kakehashi.usecase.account.ListAccountsQuery
import com.kakehashi.usecase.account.RegisterAccountUseCase
import com.kakehashi.usecase.account.SuspendAccountUseCase
import com.kakehashi.usecase.account.UnsuspendAccountUseCase
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * アカウント・ロールドメイン UseCase の DI 登録
 *
 * 根拠: docs/architecture/package-structure.md（UseCase の DI 登録）
 * APP-ADR-0008: UseCase クラスには @Service を付与しない（POJO）。
 * @Configuration + @Bean でDI コンテナに登録することで、ドメイン・ユースケース層が
 * Spring に依存しないことを型システムで保証する。
 */
@Configuration
class AccountUseCaseConfig {
    /**
     * `app.auth.google.allowed-domains` はカンマ区切りの許可ドメイン一覧（例: "example.com,example.co.jp"）。
     * 未設定・空文字の場合はドメイン制限なし（開発環境向けデフォルト。本番環境では必ず設定すること）。
     */
    @Bean
    fun googleSsoCallbackUseCase(
        repo: AccountRepository,
        googleIdTokenVerifier: GoogleIdTokenVerifier,
        jwtTokenIssuer: JwtTokenIssuer,
        @Value("\${app.auth.google.allowed-domains:}") allowedDomainsRaw: String,
    ): GoogleSsoCallbackUseCase {
        val allowedDomains = AllowedDomainsParser.parse(allowedDomainsRaw)
        return GoogleSsoCallbackUseCase(repo, googleIdTokenVerifier, jwtTokenIssuer, allowedDomains)
    }

    @Bean
    fun registerAccountUseCase(repo: AccountRepository): RegisterAccountUseCase = RegisterAccountUseCase(repo)

    @Bean
    fun editAccountUseCase(repo: AccountRepository): EditAccountUseCase = EditAccountUseCase(repo)

    @Bean
    fun assignRolesUseCase(repo: AccountRepository): AssignRolesUseCase = AssignRolesUseCase(repo)

    @Bean
    fun suspendAccountUseCase(repo: AccountRepository): SuspendAccountUseCase = SuspendAccountUseCase(repo)

    @Bean
    fun unsuspendAccountUseCase(repo: AccountRepository): UnsuspendAccountUseCase = UnsuspendAccountUseCase(repo)

    @Bean
    fun listAccountsQuery(mapper: AccountMapper): ListAccountsQuery = ListAccountsQuery(mapper)

    @Bean
    fun getAccountQuery(mapper: AccountMapper): GetAccountQuery = GetAccountQuery(mapper)
}
