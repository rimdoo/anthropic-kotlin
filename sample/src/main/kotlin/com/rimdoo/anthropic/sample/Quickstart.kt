package com.rimdoo.anthropic.sample

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.rimdoo.anthropic.Message
import com.rimdoo.anthropic.Model
import com.rimdoo.anthropic.createMessage
import com.rimdoo.anthropic.text
import kotlinx.coroutines.runBlocking

/**
 * Quickstart — single-turn chat.
 *
 * Run with:
 *   ANTHROPIC_API_KEY=sk-... ./gradlew :sample:run
 *   # or with a custom prompt:
 *   ANTHROPIC_API_KEY=sk-... ./gradlew :sample:run --args="'who are you?'"
 */
fun main(args: Array<String>): Unit = runBlocking {
    val apiKey = System.getenv("ANTHROPIC_API_KEY")
        ?: error("Set ANTHROPIC_API_KEY environment variable.")
    val prompt = args.firstOrNull() ?: "In one sentence, what is Kotlin?"

    val client = AnthropicOkHttpClient.builder()
        .apiKey(apiKey)
        .build()

    val response = client.createMessage(
        model = Model.CLAUDE_OPUS_4_7,
        maxTokens = 1024,
        messages = listOf(Message.user(prompt)),
    )

    println(response.text)
}
