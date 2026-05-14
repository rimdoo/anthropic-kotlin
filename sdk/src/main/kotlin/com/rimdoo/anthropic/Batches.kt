@file:JvmName("AnthropicKt")
@file:JvmMultifileClass

package com.rimdoo.anthropic

import com.anthropic.client.AnthropicClient
import com.anthropic.errors.AnthropicException as RawAnthropicException
import com.anthropic.models.messages.batches.BatchCreateParams
import com.anthropic.models.messages.batches.MessageBatch as RawMessageBatch
import com.anthropic.models.messages.batches.MessageBatchIndividualResponse
import com.anthropic.models.messages.batches.MessageBatchResult
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

typealias BatchProcessingStatus = RawMessageBatch.ProcessingStatus

class Batch internal constructor(
    internal val raw: RawMessageBatch,
) {
    val id: String get() = raw.id()
    val processingStatus: BatchProcessingStatus get() = raw.processingStatus()
    val createdAt: OffsetDateTime get() = raw.createdAt()
    val expiresAt: OffsetDateTime get() = raw.expiresAt()
    val endedAt: OffsetDateTime? get() = raw.endedAt().orElse(null)
    val resultsUrl: String? get() = raw.resultsUrl().orElse(null)
}

data class BatchRequest(
    val customId: String,
    val model: Model,
    val maxTokens: Int,
    val messages: List<Message>,
    val system: String? = null,
    val tools: List<Tool>? = null,
)

class BatchResult internal constructor(
    internal val raw: MessageBatchIndividualResponse,
) {
    val customId: String get() = raw.customId()
    val result: BatchResultStatus by lazy { raw.result().toKotlin() }
}

sealed class BatchResultStatus {
    data class Succeeded(val message: MessageResponse) : BatchResultStatus()
    data class Errored(val errorMessage: String) : BatchResultStatus()
    data object Canceled : BatchResultStatus()
    data object Expired : BatchResultStatus()
}

internal fun MessageBatchResult.toKotlin(): BatchResultStatus = when {
    isSucceeded() -> BatchResultStatus.Succeeded(MessageResponse(asSucceeded().message()))
    isErrored() -> BatchResultStatus.Errored(asErrored().error().toString())
    isCanceled() -> BatchResultStatus.Canceled
    isExpired() -> BatchResultStatus.Expired
    else -> BatchResultStatus.Errored("unknown result variant")
}

internal fun BatchRequest.toRaw(): BatchCreateParams.Request {
    val paramsBuilder = BatchCreateParams.Request.Params.builder()
        .model(model)
        .maxTokens(maxTokens.toLong())
        .messages(messages.map { it.raw })
    if (system != null) paramsBuilder.system(system)
    if (tools != null) paramsBuilder.tools(tools.map {
        com.anthropic.models.messages.ToolUnion.ofTool(it.raw)
    })
    return BatchCreateParams.Request.builder()
        .customId(customId)
        .params(paramsBuilder.build())
        .build()
}

suspend fun AnthropicClient.createBatch(requests: List<BatchRequest>): Batch =
    withContext(Dispatchers.IO) {
        try {
            val params = BatchCreateParams.builder().requests(requests.map { it.toRaw() }).build()
            Batch(messages().batches().create(params))
        } catch (e: RawAnthropicException) {
            throw e.toAnthropicException()
        }
    }

suspend fun AnthropicClient.retrieveBatch(batchId: String): Batch =
    withContext(Dispatchers.IO) {
        try {
            Batch(messages().batches().retrieve(batchId))
        } catch (e: RawAnthropicException) {
            throw e.toAnthropicException()
        }
    }

suspend fun AnthropicClient.cancelBatch(batchId: String): Batch =
    withContext(Dispatchers.IO) {
        try {
            Batch(messages().batches().cancel(batchId))
        } catch (e: RawAnthropicException) {
            throw e.toAnthropicException()
        }
    }

suspend fun AnthropicClient.deleteBatch(batchId: String) =
    withContext(Dispatchers.IO) {
        try {
            messages().batches().delete(batchId)
            Unit
        } catch (e: RawAnthropicException) {
            throw e.toAnthropicException()
        }
    }

fun AnthropicClient.listBatches(): Flow<Batch> = flow {
    try {
        var page = messages().batches().list()
        while (true) {
            page.data().forEach { emit(Batch(it)) }
            if (!page.hasNextPage()) break
            page = page.nextPage()
        }
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}.flowOn(Dispatchers.IO)

fun AnthropicClient.batchResults(batchId: String): Flow<BatchResult> = flow {
    try {
        messages().batches().resultsStreaming(batchId).use { stream ->
            val iter = stream.stream().iterator()
            while (iter.hasNext()) {
                emit(BatchResult(iter.next()))
            }
        }
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}.flowOn(Dispatchers.IO)
