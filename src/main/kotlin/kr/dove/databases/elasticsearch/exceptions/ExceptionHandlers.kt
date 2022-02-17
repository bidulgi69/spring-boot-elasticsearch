package kr.dove.databases.elasticsearch.exceptions

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebInputException
import java.time.ZoneId
import java.time.ZonedDateTime

@RestControllerAdvice
class ExceptionHandlers {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(NotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody fun handleNotFoundException(exchange: ServerWebExchange, ex: Exception): HttpError
            = createHttpError(HttpStatus.NOT_FOUND, exchange, ex)

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ServerWebInputException::class)
    @ResponseBody fun handleServerWebInputException(exchange: ServerWebExchange, ex: Exception): HttpError
            = createHttpError(HttpStatus.BAD_REQUEST, exchange, ex)

    private fun createHttpError(status: HttpStatus, exchange: ServerWebExchange, ex: Exception): HttpError {
        val path: String = exchange.request.path.pathWithinApplication().value()
        val message: String = ex.message ?: ex.localizedMessage

        logger.error("Error occurred. status: {}, path: {}, message: {}", status, path, message)
        exchange.response.statusCode = status
        return HttpError(path, status, message)
    }
}

data class HttpError(
    val path: String,
    val status: HttpStatus,
    val message: String,
    val timestamp: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())
)