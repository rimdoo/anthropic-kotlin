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

// -------- Vault credentials (sub-service: vaults.credentials) --------
// Note: createCredential is intentionally omitted — it requires a complex
// Auth union (api-key / oauth / etc.) that's better expressed via the raw
// Java SDK. Read / archive / delete are sufficient for most use cases.

class VaultCredential internal constructor(
    internal val raw: com.anthropic.models.beta.vaults.credentials.BetaManagedAgentsCredential,
) {
    val id: String get() = raw.id()
    val vaultId: String get() = raw.vaultId()
    val displayName: String? get() = raw.displayName().orElse(null)
    val createdAt: OffsetDateTime get() = raw.createdAt()
    val updatedAt: OffsetDateTime get() = raw.updatedAt()
    val archivedAt: OffsetDateTime? get() = raw.archivedAt().orElse(null)
}

suspend fun AnthropicClient.retrieveVaultCredential(
    vaultId: String,
    credentialId: String,
): VaultCredential = withContext(Dispatchers.IO) {
    try {
        val params = com.anthropic.models.beta.vaults.credentials.CredentialRetrieveParams.builder()
            .vaultId(vaultId)
            .credentialId(credentialId)
            .addBeta(MANAGED_AGENTS)
            .build()
        VaultCredential(beta().vaults().credentials().retrieve(params))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

suspend fun AnthropicClient.archiveVaultCredential(
    vaultId: String,
    credentialId: String,
): VaultCredential = withContext(Dispatchers.IO) {
    try {
        val params = com.anthropic.models.beta.vaults.credentials.CredentialArchiveParams.builder()
            .vaultId(vaultId)
            .credentialId(credentialId)
            .addBeta(MANAGED_AGENTS)
            .build()
        VaultCredential(beta().vaults().credentials().archive(params))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

suspend fun AnthropicClient.deleteVaultCredential(
    vaultId: String,
    credentialId: String,
) = withContext(Dispatchers.IO) {
    try {
        val params = com.anthropic.models.beta.vaults.credentials.CredentialDeleteParams.builder()
            .vaultId(vaultId)
            .credentialId(credentialId)
            .addBeta(MANAGED_AGENTS)
            .build()
        beta().vaults().credentials().delete(params)
        Unit
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

fun AnthropicClient.listVaultCredentials(vaultId: String): Flow<VaultCredential> = flow {
    try {
        val params = com.anthropic.models.beta.vaults.credentials.CredentialListParams.builder()
            .vaultId(vaultId)
            .addBeta(MANAGED_AGENTS)
            .build()
        var page = beta().vaults().credentials().list(params)
        while (true) {
            page.data().forEach { emit(VaultCredential(it)) }
            if (!page.hasNextPage()) break
            page = page.nextPage()
        }
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}.flowOn(Dispatchers.IO)

// -------- VaultCredential.create (sealed CredentialAuth) + mcpOAuthValidate --------

sealed class CredentialAuth {
    /** OAuth access token for an MCP server. */
    data class McpOAuth(val accessToken: String, val mcpServerUrl: String) : CredentialAuth()
    /** Static bearer token (e.g. long-lived API key) for an MCP server. */
    data class StaticBearer(val token: String, val mcpServerUrl: String) : CredentialAuth()
}

internal fun CredentialAuth.toRaw(): com.anthropic.models.beta.vaults.credentials.CredentialCreateParams.Auth = when (this) {
    is CredentialAuth.McpOAuth -> com.anthropic.models.beta.vaults.credentials.CredentialCreateParams.Auth.ofMcpOAuth(
        com.anthropic.models.beta.vaults.credentials.BetaManagedAgentsMcpOAuthCreateParams.builder()
            .accessToken(accessToken)
            .mcpServerUrl(mcpServerUrl)
            .build()
    )
    is CredentialAuth.StaticBearer -> com.anthropic.models.beta.vaults.credentials.CredentialCreateParams.Auth.ofStaticBearer(
        com.anthropic.models.beta.vaults.credentials.BetaManagedAgentsStaticBearerCreateParams.builder()
            .token(token)
            .mcpServerUrl(mcpServerUrl)
            .build()
    )
}

suspend fun AnthropicClient.createVaultCredential(
    vaultId: String,
    auth: CredentialAuth,
    displayName: String? = null,
): VaultCredential = withContext(Dispatchers.IO) {
    try {
        val builder = com.anthropic.models.beta.vaults.credentials.CredentialCreateParams.builder()
            .vaultId(vaultId)
            .auth(auth.toRaw())
            .addBeta(MANAGED_AGENTS)
        if (displayName != null) builder.displayName(displayName)
        VaultCredential(beta().vaults().credentials().create(builder.build()))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

class CredentialValidation internal constructor(
    internal val raw: com.anthropic.models.beta.vaults.credentials.BetaManagedAgentsCredentialValidation,
) {
    val credentialId: String get() = raw.credentialId()
    val hasRefreshToken: Boolean get() = raw.hasRefreshToken()
    val status: com.anthropic.models.beta.vaults.credentials.BetaManagedAgentsCredentialValidationStatus get() = raw.status()
}

suspend fun AnthropicClient.validateVaultCredentialMcpOAuth(
    vaultId: String,
    credentialId: String,
): CredentialValidation = withContext(Dispatchers.IO) {
    try {
        val params = com.anthropic.models.beta.vaults.credentials.CredentialMcpOAuthValidateParams.builder()
            .vaultId(vaultId)
            .credentialId(credentialId)
            .addBeta(MANAGED_AGENTS)
            .build()
        CredentialValidation(beta().vaults().credentials().mcpOAuthValidate(params))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}
