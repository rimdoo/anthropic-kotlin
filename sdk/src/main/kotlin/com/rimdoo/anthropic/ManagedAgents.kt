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

/**
 * Bypass to the Java SDK's `BetaManagedAgentsModel` so callers get typed constants like
 * `AgentModel.CLAUDE_OPUS_4_7` (and `AgentModel.of("custom-model-id")` as the escape hatch),
 * mirroring how [Model] aliases the regular Messages-API model.
 */
typealias AgentModel = BetaManagedAgentsModel

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

    /**
     * Enable only the listed tools from `agent_toolset_20260401`; every other tool in the
     * toolset is disabled via `default_config.enabled = false`. Useful when an agent should
     * only have web access, only file ops, etc.
     */
    data class Toolset20260401Subset(val enabled: Set<DefaultTool>) : AgentTool() {
        init {
            require(enabled.isNotEmpty()) {
                "Toolset20260401Subset.enabled must be non-empty — use Toolset20260401 to enable all tools"
            }
        }
    }

    /** Individual tools bundled inside `agent_toolset_20260401`. */
    enum class DefaultTool { BASH, READ, GLOB, GREP, WEB_SEARCH, WEB_FETCH }

    /**
     * Reference a previously-registered MCP server (see [McpUrlServer]) by [serverName] and
     * expose its tools to the agent. If [enabled] is null, every tool on the server is on;
     * otherwise only the listed tool names are on (everything else disabled via
     * `default_config.enabled = false`).
     */
    data class McpToolset(
        val serverName: String,
        val enabled: Set<String>? = null,
    ) : AgentTool() {
        init {
            if (enabled != null) {
                require(enabled.isNotEmpty()) {
                    "McpToolset.enabled must be non-empty — pass null to enable every tool on the server"
                }
            }
        }
    }

    /**
     * A client-side ("custom") tool. The agent emits an `agent_custom_tool_use` event when
     * it wants to call this tool; the caller must reply with [UserEvent.CustomToolResult].
     * [rawInputSchema] uses the raw Java SDK type so callers can fully describe nested
     * JSON Schema; build it with `BetaManagedAgentsCustomToolInputSchema.builder()`.
     */
    data class CustomTool(
        val name: String,
        val description: String,
        val rawInputSchema: com.anthropic.models.beta.agents.BetaManagedAgentsCustomToolInputSchema,
    ) : AgentTool()

    /**
     * Escape hatch for tool variants not modeled here, or for fine-grained configurations
     * the typed variants don't cover. Build the raw union with the Java SDK directly:
     *
     * ```kotlin
     * val raw = AgentCreateParams.Tool.ofAgentToolset20260401(
     *     BetaManagedAgentsAgentToolset20260401Params.builder()
     *         .type(BetaManagedAgentsAgentToolset20260401Params.Type.AGENT_TOOLSET_20260401)
     *         // any combination of .defaultConfig(...) and .addConfig(...) here
     *         .build()
     * )
     * client.createAgent(name = "...", model = ..., tools = listOf(AgentTool.Other(raw)))
     * ```
     */
    class Other(internal val raw: com.anthropic.models.beta.agents.AgentCreateParams.Tool) : AgentTool()
}

/**
 * URL-based remote MCP server registered on an agent (via [createAgent]'s `mcpServers`).
 * Authentication is handled separately through the agent's vault credentials — set up a
 * [Vault] with a [CredentialAuth.StaticBearer] or [CredentialAuth.McpOAuth] pointed at the
 * same `mcpServerUrl`, then attach the vault id to the session via [createSession].
 */
data class McpUrlServer(
    val name: String,
    val url: String,
)

internal fun McpUrlServer.toRaw(): com.anthropic.models.beta.agents.BetaManagedAgentsUrlMcpServerParams =
    com.anthropic.models.beta.agents.BetaManagedAgentsUrlMcpServerParams.builder()
        .type(com.anthropic.models.beta.agents.BetaManagedAgentsUrlMcpServerParams.Type.URL)
        .name(name)
        .url(url)
        .build()

internal fun AgentTool.toRaw(): com.anthropic.models.beta.agents.AgentCreateParams.Tool = when (this) {
    is AgentTool.Toolset20260401 -> com.anthropic.models.beta.agents.AgentCreateParams.Tool.ofAgentToolset20260401(
        com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolset20260401Params.builder()
            .type(com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolset20260401Params.Type.AGENT_TOOLSET_20260401)
            .build()
    )
    is AgentTool.Toolset20260401Subset -> com.anthropic.models.beta.agents.AgentCreateParams.Tool.ofAgentToolset20260401(
        com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolset20260401Params.builder()
            .type(com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolset20260401Params.Type.AGENT_TOOLSET_20260401)
            .defaultConfig(
                com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolsetDefaultConfigParams.builder()
                    .enabled(false)
                    .build()
            )
            .apply {
                enabled.forEach { tool ->
                    addConfig(
                        com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolConfigParams.builder()
                            .name(tool.toRawName())
                            .enabled(true)
                            .build()
                    )
                }
            }
            .build()
    )
    is AgentTool.McpToolset -> com.anthropic.models.beta.agents.AgentCreateParams.Tool.ofMcpToolset(
        com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolsetParams.builder()
            .type(com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolsetParams.Type.MCP_TOOLSET)
            .mcpServerName(serverName)
            .apply {
                if (enabled != null) {
                    defaultConfig(
                        com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolsetDefaultConfigParams.builder()
                            .enabled(false)
                            .build()
                    )
                    enabled.forEach { toolName ->
                        addConfig(
                            com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolConfigParams.builder()
                                .name(toolName)
                                .enabled(true)
                                .build()
                        )
                    }
                }
            }
            .build()
    )
    is AgentTool.CustomTool -> com.anthropic.models.beta.agents.AgentCreateParams.Tool.ofCustom(
        com.anthropic.models.beta.agents.BetaManagedAgentsCustomToolParams.builder()
            .type(com.anthropic.models.beta.agents.BetaManagedAgentsCustomToolParams.Type.CUSTOM)
            .name(name)
            .description(description)
            .inputSchema(rawInputSchema)
            .build()
    )
    is AgentTool.Other -> raw
}

internal fun AgentTool.DefaultTool.toRawName(): com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolConfigParams.Name = when (this) {
    AgentTool.DefaultTool.BASH -> com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolConfigParams.Name.BASH
    AgentTool.DefaultTool.READ -> com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolConfigParams.Name.READ
    AgentTool.DefaultTool.GLOB -> com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolConfigParams.Name.GLOB
    AgentTool.DefaultTool.GREP -> com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolConfigParams.Name.GREP
    AgentTool.DefaultTool.WEB_SEARCH -> com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolConfigParams.Name.WEB_SEARCH
    AgentTool.DefaultTool.WEB_FETCH -> com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolConfigParams.Name.WEB_FETCH
}

suspend fun AnthropicClient.createAgent(
    name: String,
    model: AgentModel,
    description: String? = null,
    system: String? = null,
    tools: List<AgentTool>? = null,
    mcpServers: List<McpUrlServer>? = null,
): Agent = withContext(Dispatchers.IO) {
    try {
        val builder = AgentCreateParams.builder()
            .addBeta(MANAGED_AGENTS_BETA)
            .name(name)
            .model(model)
        if (description != null) builder.description(description)
        if (system != null) builder.system(system)
        if (tools != null) builder.tools(tools.map { it.toRaw() })
        if (mcpServers != null) builder.mcpServers(mcpServers.map { it.toRaw() })
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
