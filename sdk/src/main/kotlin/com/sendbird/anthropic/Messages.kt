@file:JvmName("AnthropicKt")
@file:JvmMultifileClass

package com.sendbird.anthropic

import com.anthropic.client.AnthropicClient
import com.anthropic.errors.AnthropicException as RawAnthropicException
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.ToolUnion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun AnthropicClient.createMessage(
    model: Model,
    maxTokens: Int,
    messages: List<Message>,
    system: String? = null,
    temperature: Double? = null,
    tools: List<Tool>? = null,
): MessageResponse = withContext(Dispatchers.IO) {
    val builder = MessageCreateParams.builder()
        .model(model)
        .maxTokens(maxTokens.toLong())
        .messages(messages.map { it.raw })
    if (system != null) builder.system(system)
    if (temperature != null) builder.temperature(temperature)
    if (tools != null) builder.tools(tools.map { ToolUnion.ofTool(it.raw) })
    try {
        MessageResponse(messages().create(builder.build()))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

suspend fun AnthropicClient.createMessage(
    model: Model,
    maxTokens: Int,
    messages: List<Message>,
    system: SystemPrompt,
    temperature: Double? = null,
    tools: List<Tool>? = null,
): MessageResponse = withContext(Dispatchers.IO) {
    val builder = MessageCreateParams.builder()
        .model(model)
        .maxTokens(maxTokens.toLong())
        .messages(messages.map { it.raw })
        .system(MessageCreateParams.System.ofTextBlockParams(system.blocks))
    if (temperature != null) builder.temperature(temperature)
    if (tools != null) builder.tools(tools.map { ToolUnion.ofTool(it.raw) })
    try {
        MessageResponse(messages().create(builder.build()))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}
