package com.kakehashi.config

import com.kakehashi.domain.account.AccountRepository
import com.kakehashi.infrastructure.account.AccountMapper
import com.kakehashi.usecase.account.AssignRolesUseCase
import com.kakehashi.usecase.account.EditAccountUseCase
import com.kakehashi.usecase.account.GetAccountQuery
import com.kakehashi.usecase.account.GoogleSsoCallbackUseCase
import com.kakehashi.usecase.account.ListAccountsQuery
import com.kakehashi.usecase.account.RegisterAccountUseCase
import com.kakehashi.usecase.account.SuspendAccountUseCase
import com.kakehashi.usecase.account.UnsuspendAccountUseCase
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
    @Bean
    fun googleSsoCallbackUseCase(repo: AccountRepository): GoogleSsoCallbackUseCase = GoogleSsoCallbackUseCase(repo)

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
