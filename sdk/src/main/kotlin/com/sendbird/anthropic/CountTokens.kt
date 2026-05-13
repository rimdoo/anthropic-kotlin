@file:JvmName("AnthropicKt")
@file:JvmMultifileClass

package com.sendbird.anthropic

import com.anthropic.client.AnthropicClient
import com.anthropic.errors.AnthropicException as RawAnthropicException
import com.anthropic.models.messages.MessageCountTokensParams
import com.anthropic.models.messages.MessageTokensCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TokensCount internal constructor(
    internal val raw: MessageTokensCount,
) {
    val inputTokens: Long get() = raw.inputTokens()
}

suspend fun AnthropicClient.countMessageTokens(
    model: Model,
    messages: List<Message>,
    system: String? = null,
): TokensCount = withContext(Dispatchers.IO) {
    val builder = MessageCountTokensParams.builder()
        .model(model)
        .messages(messages.map { it.raw })
    if (system != null) builder.system(system)
    try {
        TokensCount(messages().countTokens(builder.build()))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}
