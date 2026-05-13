@file:JvmName("AnthropicKt")

package com.sendbird.anthropic

import com.anthropic.client.AnthropicClient
import com.anthropic.models.messages.MessageCreateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun AnthropicClient.createMessage(
    model: Model,
    maxTokens: Int,
    messages: List<Message>,
    system: String? = null,
    temperature: Double? = null,
): MessageResponse = withContext(Dispatchers.IO) {
    val builder = MessageCreateParams.builder()
        .model(model)
        .maxTokens(maxTokens.toLong())
        .messages(messages.map { it.raw })
    if (system != null) builder.system(system)
    if (temperature != null) builder.temperature(temperature)
    MessageResponse(messages().create(builder.build()))
}
