package com.rimdoo.anthropic.sample

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.rimdoo.anthropic.Message
import com.rimdoo.anthropic.Model
import com.rimdoo.anthropic.createMessage
import com.rimdoo.anthropic.text
import kotlinx.coroutines.runBlocking

/**
 * Get started — single-turn Messages API call.
 *
 * Mirrors the Kotlin/Java translation of the official quickstart at
 * https://platform.claude.com/docs/en/get-started.
 *
 * Run with:
 *   ANTHROPIC_API_KEY=sk-... ./gradlew :sample:run
 */
fun main(): Unit = runBlocking {
    val client = AnthropicOkHttpClient.fromEnv()

    val message = client.createMessage(
        model = Model.CLAUDE_OPUS_4_7,
        maxTokens = 1000,
        messages = listOf(
            Message.user("What should I search for to find the latest developments in renewable energy?"),
        ),
    )

    println(message.text)
}
