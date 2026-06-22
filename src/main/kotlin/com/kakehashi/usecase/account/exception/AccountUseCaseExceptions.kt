package com.kakehashi.usecase.account.exception

import com.kakehashi.domain.account.AccountStatus

/** 指定した accountId のアカウントが存在しない（404 Not Found） */
class AccountNotFoundException(
    val accountId: String,
) : RuntimeException("アカウントが見つかりません: $accountId")

/** ステータス遷移が許可されていない（409 Conflict） */
class InvalidStatusTransitionException(
    val accountId: String,
    val from: AccountStatus,
    val to: AccountStatus,
) : RuntimeException("アカウント $accountId のステータスを $from から $to へ遷移できません")

/** 楽観ロック競合（409 Conflict） */
class OptimisticLockException(
    val accountId: String,
    val requestVersion: Int,
    val currentVersion: Int,
) : RuntimeException(
        "アカウント $accountId で競合が発生しました（リクエスト version: $requestVersion、DB の現在 version: $currentVersion）",
    )

/** 認可エラー（403 Forbidden） */
class ForbiddenOperationException(
    message: String,
) : RuntimeException(message)
