package com.kakehashi.usecase.account.exception

import com.kakehashi.domain.account.AccountStatus

/** 指定した accountId のアカウントが存在しない（404 Not Found） */
class AccountNotFoundException(
    val accountId: String,
) : RuntimeException("Account not found: $accountId")

/** ステータス遷移が許可されていない（409 Conflict） */
class InvalidStatusTransitionException(
    val accountId: String,
    val from: AccountStatus,
    val to: AccountStatus,
) : RuntimeException("Cannot transition account $accountId from $from to $to")

/** 楽観ロック競合（409 Conflict） */
class OptimisticLockException(
    val accountId: String,
    val requestVersion: Int,
    val currentVersion: Int,
) : RuntimeException(
        "Optimistic lock conflict for account $accountId: " +
            "request version=$requestVersion, current version=$currentVersion",
    )

/** 認可エラー（403 Forbidden） */
class ForbiddenOperationException(
    message: String,
) : RuntimeException(message)
