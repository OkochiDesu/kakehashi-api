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
     * Google SSO コールバック（UC-A1）UseCase を DI コンテナへ登録する。
     *
     * @param accountRepository アカウント永続化のリポジトリ
     * @param googleIdTokenVerifier Google ID トークン検証ポート
     * @param jwtTokenIssuer 自前 JWT 発行・検証ポート
     * @param allowedDomainsRaw `app.auth.google.allowed-domains` の未加工値（カンマ区切りの許可ドメイン一覧、
     *   例: "example.com,example.co.jp"）。未設定・空文字の場合はパース結果が空集合となりドメイン制限なしになるが、
     *   本番相当プロファイル（test/integration-test以外）では [AuthStartupValidator] が空集合での
     *   起動を失敗させるため実質必須設定である（[AllowedDomainsParser] 参照）
     * @return 構築済みの [GoogleSsoCallbackUseCase]
     */
    @Bean
    fun googleSsoCallbackUseCase(
        accountRepository: AccountRepository,
        googleIdTokenVerifier: GoogleIdTokenVerifier,
        jwtTokenIssuer: JwtTokenIssuer,
        @Value("\${app.auth.google.allowed-domains:}") allowedDomainsRaw: String,
    ): GoogleSsoCallbackUseCase {
        val allowedDomains = AllowedDomainsParser.parse(allowedDomainsRaw)
        return GoogleSsoCallbackUseCase(accountRepository, googleIdTokenVerifier, jwtTokenIssuer, allowedDomains)
    }

    @Bean
    fun registerAccountUseCase(accountRepository: AccountRepository): RegisterAccountUseCase = RegisterAccountUseCase(accountRepository)

    @Bean
    fun editAccountUseCase(accountRepository: AccountRepository): EditAccountUseCase = EditAccountUseCase(accountRepository)

    @Bean
    fun assignRolesUseCase(accountRepository: AccountRepository): AssignRolesUseCase = AssignRolesUseCase(accountRepository)

    @Bean
    fun suspendAccountUseCase(accountRepository: AccountRepository): SuspendAccountUseCase = SuspendAccountUseCase(accountRepository)

    @Bean
    fun unsuspendAccountUseCase(accountRepository: AccountRepository): UnsuspendAccountUseCase = UnsuspendAccountUseCase(accountRepository)

    @Bean
    fun listAccountsQuery(mapper: AccountMapper): ListAccountsQuery = ListAccountsQuery(mapper)

    @Bean
    fun getAccountQuery(mapper: AccountMapper): GetAccountQuery = GetAccountQuery(mapper)
}
