package com.example.chatapp_backend.auth/controller

import com.example.chatapp_backend.auth.security.InvalidGoogleTokenException
import com.example.chatapp_backend.auth.service.AccountAlreadyExistsException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class AuthExceptionHandler {
    @ExceptionHandler(BadCredentialsException::class, InvalidGoogleTokenException::class)
    fun unauthorized(): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "invalid_credentials"))

    @ExceptionHandler(AccountAlreadyExistsException::class)
    fun conflict(ex: AccountAlreadyExistsException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to (ex.message ?: "account_conflict")))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun invalidRequest(): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("error" to "invalid_request"))
}
