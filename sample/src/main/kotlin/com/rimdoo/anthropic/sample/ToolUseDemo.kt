package com.rimdoo.anthropic.sample

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.rimdoo.anthropic.ContentBlock
import com.rimdoo.anthropic.Message
import com.rimdoo.anthropic.Model
import com.rimdoo.anthropic.Tool
import com.rimdoo.anthropic.createMessage
import com.rimdoo.anthropic.jsonSchema
import com.rimdoo.anthropic.text
import kotlinx.coroutines.runBlocking

/**
 * Tool-use demo — declares a fake weather tool, lets Claude call it, then
 * feeds the (stubbed) tool result back so Claude can produce the final answer.
 *
 * Run with:
 *   ANTHROPIC_API_KEY=sk-... ./gradlew :sample:runToolUse
 */
fun main(): Unit = runBlocking {
    val apiKey = System.getenv("ANTHROPIC_API_KEY")
        ?: error("Set ANTHROPIC_API_KEY environment variable.")

    val client = AnthropicOkHttpClient.builder()
        .apiKey(apiKey)
        .build()

    val weather = Tool(
        name = "get_weather",
        description = "Get the current weather for a city.",
        inputSchema = jsonSchema {
            property("city", type = "string", required = true)
        },
    )

    val userMessage = Message.user("What's the weather in Seoul right now?")

    // Round 1: model decides to call the tool.
    val firstResponse = client.createMessage(
        model = Model.CLAUDE_OPUS_4_7,
        maxTokens = 1024,
        tools = listOf(weather),
        messages = listOf(userMessage),
    )
    val toolUse = firstResponse.content.filterIsInstance<ContentBlock.ToolUse>().firstOrNull()
    if (toolUse == null) {
        println("Claude answered without calling the tool: ${firstResponse.text}")
        return@runBlocking
    }
    println("Claude wants to call ${toolUse.name}(${toolUse.input})")

    // Round 2: pretend we ran the tool and feed the result back.
    val followup = client.createMessage(
        model = Model.CLAUDE_OPUS_4_7,
        maxTokens = 1024,
        tools = listOf(weather),
        messages = listOf(
            userMessage,
            Message.assistant(content = firstResponse.content),
            Message.toolResult(toolUseId = toolUse.id, content = "Sunny, 21°C, light breeze"),
        ),
    )
    println(followup.text)
}
