@file:Suppress("WildcardImport")

package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@ControllerAdvice
class RestResponseEntityExceptionHandler : ResponseEntityExceptionHandler() {

    @ResponseBody
    @ExceptionHandler(value = [InvalidUrlException::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun invalidUrls(ex: InvalidUrlException) = ErrorMessage(HttpStatus.BAD_REQUEST.value(), ex.message)

    @ResponseBody
    @ExceptionHandler(value = [RedirectionNotFound::class])
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun redirectionNotFound(ex: RedirectionNotFound) = ErrorMessage(HttpStatus.NOT_FOUND.value(), ex.message)

    @ResponseBody
    @ExceptionHandler(value = [RetryAfterException::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun retryAfterException(ex: RetryAfterException): ResponseEntity<ErrorMessage> {
        val headers = HttpHeaders()
        headers.set(HttpHeaders.RETRY_AFTER, "60")
        return ResponseEntity(ErrorMessage(
                statusCode = HttpStatus.BAD_REQUEST.value(),
                message = ex.message,
                timestamp = DateTimeFormatter.ISO_DATE_TIME.format(OffsetDateTime.now())
        ), headers, HttpStatus.BAD_REQUEST)
    }

    @ResponseBody
    @ExceptionHandler(value = [RedirectionForbidden::class])
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun redirectionForbidden(ex: RedirectionForbidden) = ErrorMessage(HttpStatus.FORBIDDEN.value(), ex.message)

    @ResponseBody
    @ExceptionHandler(value = [CSVCouldNotBeProcessed::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun csvCouldNotBeProcessed(ex: CSVCouldNotBeProcessed) = ErrorMessage(HttpStatus.BAD_REQUEST.value(), ex.message)
}

data class ErrorMessage(
    val statusCode: Int,
    val message: String?,
    val timestamp: String = DateTimeFormatter.ISO_DATE_TIME.format(OffsetDateTime.now())
)
