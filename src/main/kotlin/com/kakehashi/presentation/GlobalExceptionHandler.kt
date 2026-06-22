package com.kakehashi.presentation

import com.kakehashi.usecase.account.exception.AccountNotFoundException
import com.kakehashi.usecase.account.exception.ForbiddenOperationException
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
