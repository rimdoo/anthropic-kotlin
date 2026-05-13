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
import com.anthropic.models.beta.memorystores.memories.BetaManagedAgentsMemory
import com.anthropic.models.beta.memorystores.memories.MemoryCreateParams
import com.anthropic.models.beta.memorystores.memories.MemoryDeleteParams
import com.anthropic.models.beta.memorystores.memories.MemoryListParams
import com.anthropic.models.beta.memorystores.memories.MemoryRetrieveParams
import com.anthropic.models.beta.memorystores.memories.MemoryUpdateParams
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

// -------- Memory entries (sub-service: memoryStores.memories) --------

class Memory internal constructor(
    internal val raw: BetaManagedAgentsMemory,
) {
    val id: String get() = raw.id()
    val memoryStoreId: String get() = raw.memoryStoreId()
    val memoryVersionId: String get() = raw.memoryVersionId()
    val path: String get() = raw.path()
    val content: String? get() = raw.content().orElse(null)
    val contentSha256: String get() = raw.contentSha256()
    val contentSizeBytes: Int get() = raw.contentSizeBytes()
    val createdAt: java.time.OffsetDateTime get() = raw.createdAt()
    val updatedAt: java.time.OffsetDateTime get() = raw.updatedAt()
}

suspend fun AnthropicClient.createMemory(
    memoryStoreId: String,
    path: String,
    content: String,
): Memory = withContext(Dispatchers.IO) {
    try {
        val body = MemoryCreateParams.Body.builder().path(path).content(content).build()
        val params = MemoryCreateParams.builder()
            .memoryStoreId(memoryStoreId)
            .body(body)
            .addBeta(MANAGED_AGENTS)
            .build()
        Memory(beta().memoryStores().memories().create(params))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

suspend fun AnthropicClient.retrieveMemory(
    memoryStoreId: String,
    memoryId: String,
): Memory = withContext(Dispatchers.IO) {
    try {
        val params = MemoryRetrieveParams.builder()
            .memoryStoreId(memoryStoreId)
            .memoryId(memoryId)
            .addBeta(MANAGED_AGENTS)
            .build()
        Memory(beta().memoryStores().memories().retrieve(params))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

suspend fun AnthropicClient.updateMemory(
    memoryStoreId: String,
    memoryId: String,
): Memory = withContext(Dispatchers.IO) {
    try {
        val params = MemoryUpdateParams.builder()
            .memoryStoreId(memoryStoreId)
            .memoryId(memoryId)
            .addBeta(MANAGED_AGENTS)
            .build()
        Memory(beta().memoryStores().memories().update(params))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

suspend fun AnthropicClient.deleteMemory(
    memoryStoreId: String,
    memoryId: String,
) = withContext(Dispatchers.IO) {
    try {
        val params = MemoryDeleteParams.builder()
            .memoryStoreId(memoryStoreId)
            .memoryId(memoryId)
            .addBeta(MANAGED_AGENTS)
            .build()
        beta().memoryStores().memories().delete(params)
        Unit
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

fun AnthropicClient.listMemories(memoryStoreId: String): Flow<Memory> = flow {
    try {
        val params = MemoryListParams.builder()
            .memoryStoreId(memoryStoreId)
            .addBeta(MANAGED_AGENTS)
            .build()
        var page = beta().memoryStores().memories().list(params)
        while (true) {
            page.data().forEach { item ->
                if (item.isMemory()) emit(Memory(item.asMemory()))
            }
            if (!page.hasNextPage()) break
            page = page.nextPage()
        }
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}.flowOn(Dispatchers.IO)

// -------- MemoryStore.memoryVersions (sub-service) --------

class MemoryVersion internal constructor(
    internal val raw: com.anthropic.models.beta.memorystores.memoryversions.BetaManagedAgentsMemoryVersion,
) {
    val id: String get() = raw.id()
    val memoryId: String get() = raw.memoryId()
    val memoryStoreId: String get() = raw.memoryStoreId()
    val createdAt: java.time.OffsetDateTime get() = raw.createdAt()
    val content: String? get() = raw.content().orElse(null)
    val contentSha256: String? get() = raw.contentSha256().orElse(null)
}

suspend fun AnthropicClient.retrieveMemoryVersion(
    memoryStoreId: String,
    memoryVersionId: String,
): MemoryVersion = withContext(Dispatchers.IO) {
    try {
        val params = com.anthropic.models.beta.memorystores.memoryversions.MemoryVersionRetrieveParams.builder()
            .memoryStoreId(memoryStoreId)
            .memoryVersionId(memoryVersionId)
            .addBeta(MANAGED_AGENTS)
            .build()
        MemoryVersion(beta().memoryStores().memoryVersions().retrieve(params))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

suspend fun AnthropicClient.redactMemoryVersion(
    memoryStoreId: String,
    memoryVersionId: String,
): MemoryVersion = withContext(Dispatchers.IO) {
    try {
        val params = com.anthropic.models.beta.memorystores.memoryversions.MemoryVersionRedactParams.builder()
            .memoryStoreId(memoryStoreId)
            .memoryVersionId(memoryVersionId)
            .addBeta(MANAGED_AGENTS)
            .build()
        MemoryVersion(beta().memoryStores().memoryVersions().redact(params))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

fun AnthropicClient.listMemoryVersions(memoryStoreId: String): Flow<MemoryVersion> = flow {
    try {
        val params = com.anthropic.models.beta.memorystores.memoryversions.MemoryVersionListParams.builder()
            .memoryStoreId(memoryStoreId)
            .addBeta(MANAGED_AGENTS)
            .build()
        var page = beta().memoryStores().memoryVersions().list(params)
        while (true) {
            page.data().forEach { emit(MemoryVersion(it)) }
            if (!page.hasNextPage()) break
            page = page.nextPage()
        }
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}.flowOn(Dispatchers.IO)
