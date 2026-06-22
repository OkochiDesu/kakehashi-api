package com.kakehashi.usecase.account

import com.kakehashi.domain.account.Account
import com.kakehashi.domain.account.AccountId
import com.kakehashi.domain.account.AccountRepository
import com.kakehashi.domain.account.AccountStatus
import java.time.OffsetDateTime

/**
 * UC-A1: Google SSO ログイン（仮登録・自動プロビジョニング）UseCase
 *
 * 根拠: docs/design/api/account-role.md（UC-A1）
 * - Google id_token の検証・sub ハッシュ照合はこのクラスの外（Adapter 層）で行う想定
 * - 本クラスは「sub ハッシュが確定済みの状態」から JIT プロビジョニングを行う
 * - deactivated アカウントはログイン不可（/error/deactivated へリダイレクト）
 *
 * 注意: Spring Security + Google OIDC の本格実装は別ブランチで行う。
 * 現時点では id_token の検証はスタブ実装。
 */
class GoogleSsoCallbackUseCase(
    private val accountRepository: AccountRepository,
) {
    data class Input(
        val googleSubHash: String,
        val email: String,
        val name: String,
    )

    data class Output(
        val accountId: String,
        val status: AccountStatus,
        val redirectTo: String,
    )

    /**
     * Google SSO コールバックを処理し、JIT プロビジョニングを行う。
     *
     * 設計書No：UC-A1
     * ADRNo：APP-ADR-0008
     *
     * @param input googleSubHash・email・name を含む入力値
     * @return accountId・status・リダイレクト先を含む出力値
     */
    fun execute(input: Input): Output {
        val existing = accountRepository.findByGoogleSubHash(input.googleSubHash)

        val account =
            if (existing == null) {
                // 初回ログイン: 仮登録（UC-A2 相当の内部処理）
                val seq = accountRepository.nextAccountIdSequence()
                val newAccountId = AccountId.fromSequence(seq)
                val now = OffsetDateTime.now()
                val newAccount =
                    Account(
                        accountId = newAccountId,
                        googleSubHash = input.googleSubHash,
                        email = input.email,
                        name = input.name,
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

        val redirectTo =
            when (account.status) {
                AccountStatus.PROVISIONAL -> "/registration"
                AccountStatus.ACTIVE -> "/mypage"
                AccountStatus.SUSPENDED -> "/error/suspended"
                AccountStatus.DEACTIVATED -> "/error/deactivated"
            }

        return Output(
            accountId = account.accountId.value,
            status = account.status,
            redirectTo = redirectTo,
        )
    }
}
