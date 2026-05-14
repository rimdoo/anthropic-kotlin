package com.rimdoo.anthropic.sample

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.rimdoo.anthropic.Message
import com.rimdoo.anthropic.MessageStreamEvent
import com.rimdoo.anthropic.Model
import com.rimdoo.anthropic.streamMessage
import com.rimdoo.anthropic.text
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

/**
 * Streaming demo — prints tokens as they arrive.
 *
 * Run with:
 *   ANTHROPIC_API_KEY=sk-... ./gradlew :sample:runStreaming
 */
fun main(args: Array<String>): Unit = runBlocking {
    val prompt = args.firstOrNull() ?: "Write a short haiku about Kotlin."

    val client = AnthropicOkHttpClient.fromEnv()

    client.streamMessage(
        model = Model.CLAUDE_OPUS_4_7,
        maxTokens = 1024,
        messages = listOf(Message.user(prompt)),
    ).collect { event ->
        when (event) {
            is MessageStreamEvent.ContentBlockDelta -> print(event.delta.text)
            is MessageStreamEvent.MessageStop -> println()
            else -> {}
        }
    }
}
