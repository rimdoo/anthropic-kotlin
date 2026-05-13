@file:JvmName("AnthropicKt")
@file:JvmMultifileClass

package com.sendbird.anthropic

import com.anthropic.client.AnthropicClient
import com.anthropic.errors.AnthropicException as RawAnthropicException
import com.anthropic.models.beta.AnthropicBeta
import com.anthropic.models.beta.vaults.BetaManagedAgentsVault
import com.anthropic.models.beta.vaults.VaultCreateParams
import com.anthropic.models.beta.vaults.VaultDeleteParams
import com.anthropic.models.beta.vaults.VaultListParams
import com.anthropic.models.beta.vaults.VaultRetrieveParams
import com.anthropic.models.beta.vaults.VaultUpdateParams
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

private val MANAGED_AGENTS = AnthropicBeta.MANAGED_AGENTS_2026_04_01

class Vault internal constructor(
    internal val raw: BetaManagedAgentsVault,
) {
    val id: String get() = raw.id()
    val displayName: String get() = raw.displayName()
    val createdAt: OffsetDateTime get() = raw.createdAt()
    val updatedAt: OffsetDateTime get() = raw.updatedAt()
    val archivedAt: OffsetDateTime? get() = raw.archivedAt().orElse(null)
}

suspend fun AnthropicClient.createVault(displayName: String): Vault = withContext(Dispatchers.IO) {
    try {
        val params = VaultCreateParams.builder()
            .addBeta(MANAGED_AGENTS)
            .displayName(displayName)
            .build()
        Vault(beta().vaults().create(params))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

suspend fun AnthropicClient.retrieveVault(vaultId: String): Vault = withContext(Dispatchers.IO) {
    try {
        val params = VaultRetrieveParams.builder()
            .vaultId(vaultId)
            .addBeta(MANAGED_AGENTS)
            .build()
        Vault(beta().vaults().retrieve(params))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

suspend fun AnthropicClient.updateVault(vaultId: String): Vault = withContext(Dispatchers.IO) {
    try {
        val params = VaultUpdateParams.builder()
            .vaultId(vaultId)
            .addBeta(MANAGED_AGENTS)
            .build()
        Vault(beta().vaults().update(params))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

suspend fun AnthropicClient.deleteVault(vaultId: String) = withContext(Dispatchers.IO) {
    try {
        val params = VaultDeleteParams.builder()
            .vaultId(vaultId)
            .addBeta(MANAGED_AGENTS)
            .build()
        beta().vaults().delete(params)
        Unit
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

fun AnthropicClient.listVaults(): Flow<Vault> = flow {
    try {
        val params = VaultListParams.builder().addBeta(MANAGED_AGENTS).build()
        var page = beta().vaults().list(params)
        while (true) {
            page.data().forEach { emit(Vault(it)) }
            if (!page.hasNextPage()) break
            page = page.nextPage()
        }
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}.flowOn(Dispatchers.IO)
