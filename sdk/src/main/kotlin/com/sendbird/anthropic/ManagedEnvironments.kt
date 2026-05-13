@file:JvmName("AnthropicKt")
@file:JvmMultifileClass

package com.sendbird.anthropic

import com.anthropic.client.AnthropicClient
import com.anthropic.errors.AnthropicException as RawAnthropicException
import com.anthropic.models.beta.AnthropicBeta
import com.anthropic.models.beta.environments.BetaEnvironment
import com.anthropic.models.beta.environments.EnvironmentCreateParams
import com.anthropic.models.beta.environments.EnvironmentDeleteParams
import com.anthropic.models.beta.environments.EnvironmentListParams
import com.anthropic.models.beta.environments.EnvironmentRetrieveParams
import com.anthropic.models.beta.environments.EnvironmentUpdateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

private val MANAGED_AGENTS = AnthropicBeta.MANAGED_AGENTS_2026_04_01

class Environment internal constructor(
    internal val raw: BetaEnvironment,
) {
    val id: String get() = raw.id()
    val name: String get() = raw.name()
    val description: String get() = raw.description()
    val createdAt: String get() = raw.createdAt()
    val updatedAt: String get() = raw.updatedAt()
    val archivedAt: String? get() = raw.archivedAt().orElse(null)
}

suspend fun AnthropicClient.createEnvironment(name: String): Environment = withContext(Dispatchers.IO) {
    try {
        val params = EnvironmentCreateParams.builder()
            .addBeta(MANAGED_AGENTS)
            .name(name)
            .build()
        Environment(beta().environments().create(params))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

suspend fun AnthropicClient.retrieveEnvironment(environmentId: String): Environment = withContext(Dispatchers.IO) {
    try {
        val params = EnvironmentRetrieveParams.builder()
            .environmentId(environmentId)
            .addBeta(MANAGED_AGENTS)
            .build()
        Environment(beta().environments().retrieve(params))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

suspend fun AnthropicClient.updateEnvironment(environmentId: String): Environment = withContext(Dispatchers.IO) {
    try {
        val params = EnvironmentUpdateParams.builder()
            .environmentId(environmentId)
            .addBeta(MANAGED_AGENTS)
            .build()
        Environment(beta().environments().update(params))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

suspend fun AnthropicClient.deleteEnvironment(environmentId: String) = withContext(Dispatchers.IO) {
    try {
        val params = EnvironmentDeleteParams.builder()
            .environmentId(environmentId)
            .addBeta(MANAGED_AGENTS)
            .build()
        beta().environments().delete(params)
        Unit
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

fun AnthropicClient.listEnvironments(): Flow<Environment> = flow {
    try {
        val params = EnvironmentListParams.builder().addBeta(MANAGED_AGENTS).build()
        var page = beta().environments().list(params)
        while (true) {
            page.data().forEach { emit(Environment(it)) }
            if (!page.hasNextPage()) break
            page = page.nextPage()
        }
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}.flowOn(Dispatchers.IO)
