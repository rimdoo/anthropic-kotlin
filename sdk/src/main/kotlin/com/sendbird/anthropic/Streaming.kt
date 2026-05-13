@file:JvmName("AnthropicKt")
@file:JvmMultifileClass

package com.sendbird.anthropic

import com.anthropic.client.AnthropicClient
import com.anthropic.models.messages.MessageCreateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

fun AnthropicClient.streamMessage(
    model: Model,
    maxTokens: Int,
    messages: List<Message>,
    system: String? = null,
    temperature: Double? = null,
): Flow<MessageStreamEvent> = flow {
    val builder = MessageCreateParams.builder()
        .model(model)
        .maxTokens(maxTokens.toLong())
        .messages(messages.map { it.raw })
    if (system != null) builder.system(system)
    if (temperature != null) builder.temperature(temperature)
    messages().createStreaming(builder.build()).use { stream ->
        val iter = stream.stream().iterator()
        while (iter.hasNext()) {
            emit(iter.next().toKotlin())
        }
    }
}.flowOn(Dispatchers.IO)
