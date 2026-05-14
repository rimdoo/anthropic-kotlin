package com.rimdoo.anthropic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds

/** Each AnthropicException subtype is publicly visible — make sure each one can be
 *  instantiated and exposes the fields the public Kotlin API documents. */
class ExceptionsTest {

    private val cause = RuntimeException("server said no")

    @Test fun `RateLimit carries retryAfter and statusCode`() {
        val e = AnthropicException.RateLimit(retryAfter = 30.seconds, statusCode = 429, cause = cause)
        assertEquals(30.seconds, e.retryAfter)
        assertEquals(429, e.statusCode)
        assertEquals(cause, e.cause)
        assertIs<RuntimeException>(e)
    }

    @Test fun `Auth carries statusCode`() {
        val e = AnthropicException.Auth(statusCode = 401, cause = cause)
        assertEquals(401, e.statusCode)
        assertEquals(cause, e.cause)
    }

    @Test fun `BadRequest carries message and statusCode`() {
        val e = AnthropicException.BadRequest(statusCode = 400, message = "malformed", cause = cause)
        assertEquals(400, e.statusCode)
        assertEquals("malformed", e.message)
    }

    @Test fun `NotFound carries statusCode`() {
        val e = AnthropicException.NotFound(statusCode = 404, cause = cause)
        assertEquals(404, e.statusCode)
    }

    @Test fun `Unprocessable carries message and statusCode`() {
        val e = AnthropicException.Unprocessable(statusCode = 422, message = "invalid", cause = cause)
        assertEquals(422, e.statusCode)
        assertEquals("invalid", e.message)
    }

    @Test fun `Server carries statusCode`() {
        val e = AnthropicException.Server(statusCode = 502, cause = cause)
        assertEquals(502, e.statusCode)
    }

    @Test fun `Unknown carries cause and message`() {
        val e = AnthropicException.Unknown(cause = cause)
        assertEquals(cause, e.cause)
        assertEquals(cause.message, e.message)
    }
}
