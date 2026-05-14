@file:JvmName("AnthropicKt")
@file:JvmMultifileClass
@file:Suppress("DEPRECATION")

package com.rimdoo.anthropic

import com.anthropic.client.AnthropicClient
import com.anthropic.errors.AnthropicException as RawAnthropicException
import com.anthropic.models.completions.Completion as RawCompletion
import com.anthropic.models.completions.CompletionCreateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class CompletionResponse internal constructor(
    internal val raw: RawCompletion,
) {
    val id: String get() = raw.id()
    val completion: String get() = raw.completion()
    val stopReason: String? get() = raw.stopReason().orElse(null)
}

private fun buildCompletionParams(
    model: Model,
    prompt: String,
    maxTokensToSample: Int,
    temperature: Double?,
    stopSequences: List<String>?,
): CompletionCreateParams {
    val b = CompletionCreateParams.builder()
        .model(model)
        .prompt(prompt)
        .maxTokensToSample(maxTokensToSample.toLong())
    if (temperature != null) b.temperature(temperature)
    if (stopSequences != null) b.stopSequences(stopSequences)
    return b.build()
}

suspend fun AnthropicClient.createCompletion(
    model: Model,
    prompt: String,
    maxTokensToSample: Int,
    temperature: Double? = null,
    stopSequences: List<String>? = null,
): CompletionResponse = withContext(Dispatchers.IO) {
    try {
        CompletionResponse(
            completions().create(
                buildCompletionParams(model, prompt, maxTokensToSample, temperature, stopSequences)
            )
        )
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

fun AnthropicClient.streamCompletion(
    model: Model,
    prompt: String,
    maxTokensToSample: Int,
    temperature: Double? = null,
    stopSequences: List<String>? = null,
): Flow<CompletionResponse> = flow {
    try {
        completions().createStreaming(
            buildCompletionParams(model, prompt, maxTokensToSample, temperature, stopSequences)
        ).use { stream ->
            val iter = stream.stream().iterator()
            while (iter.hasNext()) {
                emit(CompletionResponse(iter.next()))
            }
        }
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}.flowOn(Dispatchers.IO)
