@file:JvmName("AnthropicKt")
@file:JvmMultifileClass

package com.rimdoo.anthropic

import com.anthropic.client.AnthropicClient
import com.anthropic.errors.AnthropicException as RawAnthropicException
import com.anthropic.models.beta.AnthropicBeta
import com.anthropic.models.beta.agents.AgentArchiveParams
import com.anthropic.models.beta.agents.AgentCreateParams
import com.anthropic.models.beta.agents.AgentListParams
import com.anthropic.models.beta.agents.AgentRetrieveParams
import com.anthropic.models.beta.agents.AgentUpdateParams
import com.anthropic.models.beta.agents.BetaManagedAgentsAgent
import com.anthropic.models.beta.agents.BetaManagedAgentsModel
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

private val MANAGED_AGENTS_BETA = AnthropicBeta.MANAGED_AGENTS_2026_04_01

class Agent internal constructor(
    internal val raw: BetaManagedAgentsAgent,
) {
    val id: String get() = raw.id()
    val name: String get() = raw.name()
    val description: String? get() = raw.description().orElse(null)
    val system: String? get() = raw.system().orElse(null)
    val createdAt: OffsetDateTime get() = raw.createdAt()
    val updatedAt: OffsetDateTime get() = raw.updatedAt()
    val archivedAt: OffsetDateTime? get() = raw.archivedAt().orElse(null)
    val version: Int get() = raw.version()
}

/**
 * Tools an agent has access to. The `agent_toolset_20260401` toolset is the curated default
 * set (bash, file ops, web search, etc.); MCP toolsets and custom tools are advanced/raw-only
 * for now and can be added via [Other] until they're modeled here.
 */
sealed class AgentTool {
    /** Enable the full bundled agent toolset (bash, write, web search, etc.). */
    data object Toolset20260401 : AgentTool()
    /** Escape hatch for MCP or custom tool variants — pass the raw Java SDK union. */
    class Other internal constructor(
        internal val raw: com.anthropic.models.beta.agents.AgentCreateParams.Tool,
    ) : AgentTool()
}

internal fun AgentTool.toRaw(): com.anthropic.models.beta.agents.AgentCreateParams.Tool = when (this) {
    is AgentTool.Toolset20260401 -> com.anthropic.models.beta.agents.AgentCreateParams.Tool.ofAgentToolset20260401(
        com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolset20260401Params.builder().build()
    )
    is AgentTool.Other -> raw
}

suspend fun AnthropicClient.createAgent(
    name: String,
    model: String,
    description: String? = null,
    system: String? = null,
    tools: List<AgentTool>? = null,
): Agent = withContext(Dispatchers.IO) {
    try {
        val builder = AgentCreateParams.builder()
            .addBeta(MANAGED_AGENTS_BETA)
            .name(name)
            .model(BetaManagedAgentsModel.of(model))
        if (description != null) builder.description(description)
        if (system != null) builder.system(system)
        if (tools != null) builder.tools(tools.map { it.toRaw() })
        Agent(beta().agents().create(builder.build()))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

suspend fun AnthropicClient.retrieveAgent(agentId: String): Agent = withContext(Dispatchers.IO) {
    try {
        val params = AgentRetrieveParams.builder()
            .agentId(agentId)
            .addBeta(MANAGED_AGENTS_BETA)
            .build()
        Agent(beta().agents().retrieve(params))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

suspend fun AnthropicClient.archiveAgent(agentId: String): Agent = withContext(Dispatchers.IO) {
    try {
        val params = AgentArchiveParams.builder()
            .agentId(agentId)
            .addBeta(MANAGED_AGENTS_BETA)
            .build()
        Agent(beta().agents().archive(params))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

suspend fun AnthropicClient.updateAgent(
    agentId: String,
    version: Int,
): Agent = withContext(Dispatchers.IO) {
    try {
        val params = AgentUpdateParams.builder()
            .agentId(agentId)
            .version(version)
            .addBeta(MANAGED_AGENTS_BETA)
            .build()
        Agent(beta().agents().update(params))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

fun AnthropicClient.listAgents(): Flow<Agent> = flow {
    try {
        val params = AgentListParams.builder().addBeta(MANAGED_AGENTS_BETA).build()
        var page = beta().agents().list(params)
        while (true) {
            page.data().forEach { emit(Agent(it)) }
            if (!page.hasNextPage()) break
            page = page.nextPage()
        }
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}.flowOn(Dispatchers.IO)

// -------- Agent versions (sub-service: agents.versions) --------

fun AnthropicClient.listAgentVersions(agentId: String): Flow<Agent> = flow {
    try {
        val params = com.anthropic.models.beta.agents.versions.VersionListParams.builder()
            .agentId(agentId)
            .addBeta(MANAGED_AGENTS_BETA)
            .build()
        var page = beta().agents().versions().list(params)
        while (true) {
            page.data().forEach { emit(Agent(it)) }
            if (!page.hasNextPage()) break
            page = page.nextPage()
        }
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}.flowOn(Dispatchers.IO)
