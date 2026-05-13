@file:JvmName("AnthropicKt")
@file:JvmMultifileClass
@file:Suppress("DEPRECATION")

package com.sendbird.anthropic

import com.anthropic.client.AnthropicClient
import com.anthropic.errors.AnthropicException as RawAnthropicException
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.ToolUnion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun MessageCreateParams.Builder.applyCommonOptions(
    temperature: Double?,
    tools: List<Tool>?,
    thinking: ThinkingConfig?,
    topK: Int?,
    topP: Double?,
    stopSequences: List<String>?,
): MessageCreateParams.Builder = apply {
    if (temperature != null) temperature(temperature)
    if (tools != null) tools(tools.map { ToolUnion.ofTool(it.raw) })
    if (thinking != null) thinking(thinking.toRaw())
    if (topK != null) topK(topK.toLong())
    if (topP != null) topP(topP)
    if (stopSequences != null) stopSequences(stopSequences)
}

suspend fun AnthropicClient.createMessage(
    model: Model,
    maxTokens: Int,
    messages: List<Message>,
    system: String? = null,
    temperature: Double? = null,
    tools: List<Tool>? = null,
    thinking: ThinkingConfig? = null,
    topK: Int? = null,
    topP: Double? = null,
    stopSequences: List<String>? = null,
): MessageResponse = withContext(Dispatchers.IO) {
    val builder = MessageCreateParams.builder()
        .model(model)
        .maxTokens(maxTokens.toLong())
        .messages(messages.map { it.raw })
        .applyCommonOptions(temperature, tools, thinking, topK, topP, stopSequences)
    if (system != null) builder.system(system)
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
    thinking: ThinkingConfig? = null,
    topK: Int? = null,
    topP: Double? = null,
    stopSequences: List<String>? = null,
): MessageResponse = withContext(Dispatchers.IO) {
    val builder = MessageCreateParams.builder()
        .model(model)
        .maxTokens(maxTokens.toLong())
        .messages(messages.map { it.raw })
        .system(MessageCreateParams.System.ofTextBlockParams(system.blocks))
        .applyCommonOptions(temperature, tools, thinking, topK, topP, stopSequences)
    try {
        MessageResponse(messages().create(builder.build()))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}
