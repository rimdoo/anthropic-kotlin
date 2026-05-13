@file:JvmName("AnthropicKt")
@file:JvmMultifileClass

package com.sendbird.anthropic

import com.anthropic.client.AnthropicClient
import com.anthropic.errors.AnthropicException as RawAnthropicException
import com.anthropic.models.beta.AnthropicBeta
import com.anthropic.models.beta.memorystores.BetaManagedAgentsMemoryStore
import com.anthropic.models.beta.memorystores.MemoryStoreCreateParams
import com.anthropic.models.beta.memorystores.MemoryStoreListParams
import com.anthropic.models.beta.memorystores.MemoryStoreRetrieveParams
import com.anthropic.models.beta.memorystores.MemoryStoreUpdateParams
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

private val MANAGED_AGENTS = AnthropicBeta.MANAGED_AGENTS_2026_04_01

class MemoryStore internal constructor(
    internal val raw: BetaManagedAgentsMemoryStore,
) {
    val id: String get() = raw.id()
    val name: String get() = raw.name()
    val description: String? get() = raw.description().orElse(null)
    val createdAt: OffsetDateTime get() = raw.createdAt()
    val updatedAt: OffsetDateTime get() = raw.updatedAt()
    val archivedAt: OffsetDateTime? get() = raw.archivedAt().orElse(null)
}

suspend fun AnthropicClient.createMemoryStore(
    name: String,
    description: String? = null,
): MemoryStore = withContext(Dispatchers.IO) {
    try {
        val builder = MemoryStoreCreateParams.builder()
            .addBeta(MANAGED_AGENTS)
            .name(name)
        if (description != null) builder.description(description)
        MemoryStore(beta().memoryStores().create(builder.build()))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

suspend fun AnthropicClient.retrieveMemoryStore(memoryStoreId: String): MemoryStore = withContext(Dispatchers.IO) {
    try {
        val params = MemoryStoreRetrieveParams.builder()
            .memoryStoreId(memoryStoreId)
            .addBeta(MANAGED_AGENTS)
            .build()
        MemoryStore(beta().memoryStores().retrieve(params))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

suspend fun AnthropicClient.updateMemoryStore(memoryStoreId: String): MemoryStore = withContext(Dispatchers.IO) {
    try {
        val params = MemoryStoreUpdateParams.builder()
            .memoryStoreId(memoryStoreId)
            .addBeta(MANAGED_AGENTS)
            .build()
        MemoryStore(beta().memoryStores().update(params))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

fun AnthropicClient.listMemoryStores(): Flow<MemoryStore> = flow {
    try {
        val params = MemoryStoreListParams.builder().addBeta(MANAGED_AGENTS).build()
        var page = beta().memoryStores().list(params)
        while (true) {
            page.data().forEach { emit(MemoryStore(it)) }
            if (!page.hasNextPage()) break
            page = page.nextPage()
        }
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}.flowOn(Dispatchers.IO)
