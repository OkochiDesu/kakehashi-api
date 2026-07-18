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

/**
 * Google ID トークンの署名検証に失敗（401 Unauthorized）
 *
 * 根拠: docs/design/api/account-role.md（UC-A1）、APP-ADR-0014
 * Google JWKS による署名・iss/aud/有効期限の検証に失敗した場合にスローする。
 */
class GoogleIdTokenVerificationException(
    message: String,
) : RuntimeException(message)

/**
 * idToken のフォーマットが JWT として不正（422 Unprocessable Entity）
 *
 * 根拠: docs/design/api/account-role.md（UC-A1）、APP-ADR-0014
 */
class InvalidIdTokenFormatException(
    message: String,
) : RuntimeException(message)

/**
 * 許可された会社ドメイン以外の Google アカウント（422 Unprocessable Entity）
 *
 * 根拠: docs/design/api/account-role.md（UC-A1）、APP-ADR-0014
 */
class DomainNotAllowedException(
    message: String,
) : RuntimeException(message)
