@file:JvmName("AnthropicKt")
@file:JvmMultifileClass

package com.sendbird.anthropic

import com.anthropic.client.AnthropicClient
import com.anthropic.errors.AnthropicException as RawAnthropicException
import com.anthropic.models.models.ModelInfo as RawModelInfo
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class ModelInfo internal constructor(
    internal val raw: RawModelInfo,
) {
    val id: String get() = raw.id()
    val displayName: String get() = raw.displayName()
    val createdAt: OffsetDateTime get() = raw.createdAt()
    val maxInputTokens: Long? get() = raw.maxInputTokens().orElse(null)
    val maxTokens: Long? get() = raw.maxTokens().orElse(null)
}

fun AnthropicClient.listModels(): Flow<ModelInfo> = flow {
    try {
        var page = models().list()
        while (true) {
            page.data().forEach { emit(ModelInfo(it)) }
            if (!page.hasNextPage()) break
            page = page.nextPage()
        }
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}.flowOn(Dispatchers.IO)

suspend fun AnthropicClient.retrieveModel(modelId: String): ModelInfo = withContext(Dispatchers.IO) {
    try {
        ModelInfo(models().retrieve(modelId))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}
