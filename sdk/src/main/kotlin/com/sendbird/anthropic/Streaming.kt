@file:JvmName("AnthropicKt")
@file:JvmMultifileClass
@file:Suppress("DEPRECATION")

package com.sendbird.anthropic

import com.anthropic.client.AnthropicClient
import com.anthropic.errors.AnthropicException as RawAnthropicException
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.ToolUnion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

private fun MessageCreateParams.Builder.applyStreamOptions(
    temperature: Double?,
    tools: List<Tool>?,
    toolChoice: ToolChoice?,
    thinking: ThinkingConfig?,
    topK: Int?,
    topP: Double?,
    stopSequences: List<String>?,
): MessageCreateParams.Builder = apply {
    if (temperature != null) temperature(temperature)
    if (tools != null) tools(tools.map { ToolUnion.ofTool(it.raw) })
    if (toolChoice != null) toolChoice(toolChoice.toRaw())
    if (thinking != null) thinking(thinking.toRaw())
    if (topK != null) topK(topK.toLong())
    if (topP != null) topP(topP)
    if (stopSequences != null) stopSequences(stopSequences)
}

fun AnthropicClient.streamMessage(
    model: Model,
    maxTokens: Int,
    messages: List<Message>,
    system: String? = null,
    temperature: Double? = null,
    tools: List<Tool>? = null,
    toolChoice: ToolChoice? = null,
    thinking: ThinkingConfig? = null,
    topK: Int? = null,
    topP: Double? = null,
    stopSequences: List<String>? = null,
): Flow<MessageStreamEvent> = flow {
    val builder = MessageCreateParams.builder()
        .model(model)
        .maxTokens(maxTokens.toLong())
        .messages(messages.map { it.raw })
        .applyStreamOptions(temperature, tools, toolChoice, thinking, topK, topP, stopSequences)
    if (system != null) builder.system(system)
    try {
        messages().createStreaming(builder.build()).use { stream ->
            val iter = stream.stream().iterator()
            while (iter.hasNext()) {
                emit(iter.next().toKotlin())
            }
        }
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}.flowOn(Dispatchers.IO)

fun AnthropicClient.streamMessage(
    model: Model,
    maxTokens: Int,
    messages: List<Message>,
    system: SystemPrompt,
    temperature: Double? = null,
    tools: List<Tool>? = null,
    toolChoice: ToolChoice? = null,
    thinking: ThinkingConfig? = null,
    topK: Int? = null,
    topP: Double? = null,
    stopSequences: List<String>? = null,
): Flow<MessageStreamEvent> = flow {
    val builder = MessageCreateParams.builder()
        .model(model)
        .maxTokens(maxTokens.toLong())
        .messages(messages.map { it.raw })
        .system(MessageCreateParams.System.ofTextBlockParams(system.blocks))
        .applyStreamOptions(temperature, tools, toolChoice, thinking, topK, topP, stopSequences)
    try {
        messages().createStreaming(builder.build()).use { stream ->
            val iter = stream.stream().iterator()
            while (iter.hasNext()) {
                emit(iter.next().toKotlin())
            }
        }
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}.flowOn(Dispatchers.IO)
