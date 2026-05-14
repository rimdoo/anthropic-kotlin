package com.rimdoo.anthropic

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import com.anthropic.errors.AnthropicException as RawAnthropicException
import com.anthropic.errors.BadRequestException as RawBadRequestException
import com.anthropic.errors.InternalServerException as RawInternalServerException
import com.anthropic.errors.NotFoundException as RawNotFoundException
import com.anthropic.errors.RateLimitException as RawRateLimitException
import com.anthropic.errors.UnauthorizedException as RawUnauthorizedException
import com.anthropic.errors.UnprocessableEntityException as RawUnprocessableEntityException

sealed class AnthropicException(
    message: String?,
    cause: Throwable?,
) : RuntimeException(message, cause) {

    /** 429 — too many requests. */
    class RateLimit(
        val retryAfter: Duration,
        val statusCode: Int,
        cause: Throwable,
    ) : AnthropicException("rate limited (retry after $retryAfter)", cause)

    /** 401 — invalid or missing API key. */
    class Auth(
        val statusCode: Int,
        cause: Throwable,
    ) : AnthropicException("authentication failed", cause)

    /** 400 — malformed request. */
    class BadRequest(
        val statusCode: Int,
        message: String,
        cause: Throwable,
    ) : AnthropicException(message, cause)

    /** 404 — resource not found. */
    class NotFound(
        val statusCode: Int,
        cause: Throwable,
    ) : AnthropicException("not found", cause)

    /** 422 — request well-formed but semantically invalid. */
    class Unprocessable(
        val statusCode: Int,
        message: String,
        cause: Throwable,
    ) : AnthropicException(message, cause)

    /** 5xx — server-side error. */
    class Server(
        val statusCode: Int,
        cause: Throwable,
    ) : AnthropicException("server error ($statusCode)", cause)

    /** Anything else we couldn't classify. */
    class Unknown(cause: Throwable) : AnthropicException(cause.message, cause)
}

internal fun Throwable.toAnthropicException(): AnthropicException = when (this) {
    is RawRateLimitException -> {
        val retryAfter = headers().values("Retry-After").firstOrNull()
            ?.toLongOrNull()?.seconds
            ?: 60.seconds
        AnthropicException.RateLimit(retryAfter, statusCode(), this)
    }
    is RawUnauthorizedException -> AnthropicException.Auth(statusCode(), this)
    is RawBadRequestException -> AnthropicException.BadRequest(statusCode(), message ?: "bad request", this)
    is RawNotFoundException -> AnthropicException.NotFound(statusCode(), this)
    is RawUnprocessableEntityException -> AnthropicException.Unprocessable(statusCode(), message ?: "unprocessable", this)
    is RawInternalServerException -> AnthropicException.Server(statusCode(), this)
    is RawAnthropicException -> AnthropicException.Unknown(this)
    else -> AnthropicException.Unknown(this)
}
