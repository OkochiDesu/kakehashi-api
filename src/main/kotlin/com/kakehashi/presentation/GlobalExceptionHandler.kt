package com.kakehashi.presentation

import com.kakehashi.usecase.account.exception.AccountNotFoundException
import com.kakehashi.usecase.account.exception.DomainNotAllowedException
import com.kakehashi.usecase.account.exception.ForbiddenOperationException
import com.kakehashi.usecase.account.exception.GoogleIdTokenVerificationException
import com.kakehashi.usecase.account.exception.InvalidIdTokenFormatException
import com.kakehashi.usecase.account.exception.InvalidStatusTransitionException
import com.kakehashi.usecase.account.exception.OptimisticLockException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * グローバル例外ハンドラ
 *
 * 根拠: CLAUDE.md（エラーハンドリングは @ControllerAdvice で一元管理）
 * 各 UseCase が throw した業務例外を HTTP レスポンスに変換する
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    data class ErrorResponse(
        val code: String,
        val message: String,
    )

    /** 404 Not Found */
    @ExceptionHandler(AccountNotFoundException::class)
    fun handleNotFound(ex: AccountNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(code = "ACCOUNT_NOT_FOUND", message = ex.message ?: "アカウントが見つかりません"))

    /**
     * 409 Conflict — 楽観ロック競合
     * APP-ADR-0005: version 不一致は 409 Conflict
     */
    @ExceptionHandler(OptimisticLockException::class)
    fun handleOptimisticLock(ex: OptimisticLockException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse(code = "OPTIMISTIC_LOCK_CONFLICT", message = ex.message ?: "バージョン競合が発生しました"))

    /**
     * 409 Conflict — ステータス遷移不可
     * 二重申込み防止・既に停止中の場合など
     */
    @ExceptionHandler(InvalidStatusTransitionException::class)
    fun handleInvalidTransition(ex: InvalidStatusTransitionException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                ErrorResponse(
                    code = "INVALID_STATUS_TRANSITION",
                    message = ex.message ?: "ステータス遷移が許可されていません",
                ),
            )

    /** 403 Forbidden — 認可エラー */
    @ExceptionHandler(ForbiddenOperationException::class)
    fun handleForbidden(ex: ForbiddenOperationException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ErrorResponse(code = "FORBIDDEN", message = ex.message ?: "この操作を実行する権限がありません"))

    /**
     * 401 Unauthorized — Google ID トークンの署名検証失敗
     * APP-ADR-0014: Google JWKS による署名・iss/aud/有効期限の検証に失敗した場合
     */
    @ExceptionHandler(GoogleIdTokenVerificationException::class)
    fun handleGoogleIdTokenVerification(ex: GoogleIdTokenVerificationException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(
                ErrorResponse(
                    code = "GOOGLE_ID_TOKEN_VERIFICATION_FAILED",
                    message = ex.message ?: "Google IDトークンの検証に失敗しました",
                ),
            )

    /**
     * 422 Unprocessable Entity — idToken のフォーマット不正
     * APP-ADR-0014: idToken が JWT として解析できない場合
     */
    @ExceptionHandler(InvalidIdTokenFormatException::class)
    fun handleInvalidIdTokenFormat(ex: InvalidIdTokenFormatException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(
                ErrorResponse(
                    code = "INVALID_ID_TOKEN_FORMAT",
                    message = ex.message ?: "idTokenのフォーマットが不正です",
                ),
            )

    /**
     * 422 Unprocessable Entity — 許可ドメイン外の Google アカウント
     * APP-ADR-0014: 環境変数で指定した会社ドメイン以外の Google アカウントによるログイン試行
     */
    @ExceptionHandler(DomainNotAllowedException::class)
    fun handleDomainNotAllowed(ex: DomainNotAllowedException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(
                ErrorResponse(
                    code = "DOMAIN_NOT_ALLOWED",
                    message = ex.message ?: "許可されていないドメインのアカウントです",
                ),
            )

    /** 400 Bad Request — バリデーションエラー（@Valid） */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val details =
            ex.bindingResult.fieldErrors
                .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(code = "VALIDATION_ERROR", message = details))
    }

    /** 400 Bad Request — AccountId フォーマット不正など */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(code = "BAD_REQUEST", message = ex.message ?: "不正なリクエストです"))
}
