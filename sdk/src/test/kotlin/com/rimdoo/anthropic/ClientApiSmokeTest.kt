package com.rimdoo.anthropic

import com.anthropic.client.AnthropicClient
import io.mockk.mockk
import kotlin.test.Test
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runTest

/**
 * Smoke test every public extension function on [AnthropicClient] by invoking it against
 * a [mockk] stub. The stub auto-returns relaxed mocks for every chained call, so the
 * suspend wrapper executes its full Builder chain (including all `.type()` discriminators)
 * before reaching the mocked sub-service.
 *
 * Any missing-required-field bug in a Builder chain — the same class as the
 * `Toolset20260401Params type required` runtime crash from 2026-05-13 — surfaces here
 * as an `IllegalStateException` and fails the corresponding test.
 *
 * Note: these tests do NOT assert on the result — `relaxed = true` returns empty defaults
 * (empty lists, false booleans, "" strings) for everything. The contract is "call without
 * throwing".
 */
class ClientApiSmokeTest {

    private fun client(): AnthropicClient = mockk(relaxed = true)

    // ============================== Messages ==============================

    @Test fun `createMessage minimum`() = runTest {
        client().createMessage(
            model = Model.CLAUDE_OPUS_4_7,
            maxTokens = 100,
            messages = listOf(Message.user("hi")),
        )
    }

    @Test fun `createMessage with String system + every option`() = runTest {
        client().createMessage(
            model = Model.CLAUDE_OPUS_4_7,
            maxTokens = 100,
            messages = listOf(Message.user("hi")),
            system = "be helpful",
            temperature = 0.7,
            tools = listOf(
                Tool(name = "w", description = "weather",
                    inputSchema = jsonSchema { property("city", type = "string", required = true) }),
            ),
            toolChoice = ToolChoice.Auto,
            thinking = ThinkingConfig.Enabled(budgetTokens = 1024),
            topK = 40,
            topP = 0.9,
            stopSequences = listOf("\n\nHuman:"),
        )
    }

    @Test fun `createMessage with SystemPrompt overload`() = runTest {
        client().createMessage(
            model = Model.CLAUDE_OPUS_4_7,
            maxTokens = 100,
            messages = listOf(Message.user("hi")),
            system = systemPrompt {
                text("long prefix", cache = CacheControl.Ephemeral)
            },
            toolChoice = ToolChoice.Tool(name = "w"),
        )
    }

    @Test fun `streamMessage with String system`() = runTest {
        client().streamMessage(
            model = Model.CLAUDE_OPUS_4_7,
            maxTokens = 100,
            messages = listOf(Message.user("hi")),
            system = "be helpful",
        ).collect()
    }

    @Test fun `streamMessage with SystemPrompt + every option`() = runTest {
        client().streamMessage(
            model = Model.CLAUDE_OPUS_4_7,
            maxTokens = 100,
            messages = listOf(Message.user("hi")),
            system = systemPrompt { text("ctx", cache = CacheControl.Ephemeral) },
            temperature = 0.5,
            tools = listOf(
                Tool(name = "w", description = "weather",
                    inputSchema = jsonSchema { property("city", type = "string") }),
            ),
            toolChoice = ToolChoice.Any,
            thinking = ThinkingConfig.Disabled,
            topK = 10,
            topP = 0.95,
            stopSequences = listOf("END"),
        ).collect()
    }

    @Test fun `countMessageTokens`() = runTest {
        client().countMessageTokens(
            model = Model.CLAUDE_OPUS_4_7,
            messages = listOf(Message.user("hi")),
            system = "be helpful",
        )
    }

    // ============================== Completions ==============================

    @Test fun `createCompletion`() = runTest {
        client().createCompletion(
            model = Model.CLAUDE_OPUS_4_7,
            prompt = "\n\nHuman: hi\n\nAssistant:",
            maxTokensToSample = 256,
            temperature = 0.7,
            stopSequences = listOf("\n\nHuman:"),
        )
    }

    @Test fun `streamCompletion`() = runTest {
        client().streamCompletion(
            model = Model.CLAUDE_OPUS_4_7,
            prompt = "hi",
            maxTokensToSample = 64,
        ).collect()
    }

    // ============================== Batches ==============================

    @Test fun `createBatch`() = runTest {
        client().createBatch(
            requests = listOf(
                BatchRequest(
                    customId = "req-1",
                    model = Model.CLAUDE_OPUS_4_7,
                    maxTokens = 100,
                    messages = listOf(Message.user("a")),
                    system = "be helpful",
                    tools = listOf(
                        Tool("w", "weather", jsonSchema { property("city", type = "string") }),
                    ),
                ),
            ),
        )
    }

    @Test fun `retrieveBatch`() = runTest { client().retrieveBatch("batch_abc") }
    @Test fun `cancelBatch`()   = runTest { client().cancelBatch("batch_abc") }
    @Test fun `deleteBatch`()   = runTest { client().deleteBatch("batch_abc") }
    @Test fun `listBatches`()   = runTest { client().listBatches().collect() }
    @Test fun `batchResults`()  = runTest { client().batchResults("batch_abc").collect() }

    // ============================== Models ==============================

    @Test fun `listModels`()    = runTest { client().listModels().collect() }
    @Test fun `retrieveModel`() = runTest { client().retrieveModel("claude-opus-4-7") }

    // ============================== Beta Files ==============================

    @Test fun `uploadFile`()              = runTest { client().uploadFile(bytes = "hello".toByteArray()) }
    @Test fun `retrieveFileMetadata`()    = runTest { client().retrieveFileMetadata("file_x") }
    @Test fun `deleteFile`()              = runTest { client().deleteFile("file_x") }
    @Test fun `listFiles`()               = runTest { client().listFiles().collect() }

    // ============================== Agents ==============================

    @Test fun `createAgent minimum`() = runTest {
        client().createAgent(name = "x", model = AgentModel.CLAUDE_OPUS_4_7)
    }

    @Test fun `createAgent with toolset and system`() = runTest {
        client().createAgent(
            name = "x",
            model = AgentModel.CLAUDE_OPUS_4_7,
            description = "desc",
            system = "sys",
            tools = listOf(AgentTool.Toolset20260401),
        )
    }

    @Test fun `retrieveAgent`() = runTest { client().retrieveAgent("agent_x") }
    @Test fun `updateAgent`()   = runTest { client().updateAgent("agent_x", version = 1) }
    @Test fun `archiveAgent`()  = runTest { client().archiveAgent("agent_x") }
    @Test fun `listAgents`()    = runTest { client().listAgents().collect() }
    @Test fun `listAgentVersions`() = runTest { client().listAgentVersions("agent_x").collect() }

    // ============================== Environments ==============================

    @Test fun `createEnvironment default unrestricted`() = runTest {
        client().createEnvironment(name = "env-x")
    }

    @Test fun `createEnvironment explicit unrestricted`() = runTest {
        client().createEnvironment(name = "env-x", networking = NetworkPolicy.Unrestricted)
    }

    @Test fun `retrieveEnvironment`() = runTest { client().retrieveEnvironment("env_x") }
    @Test fun `updateEnvironment`()   = runTest { client().updateEnvironment("env_x") }
    @Test fun `deleteEnvironment`()   = runTest { client().deleteEnvironment("env_x") }
    @Test fun `listEnvironments`()    = runTest { client().listEnvironments().collect() }

    // ============================== Sessions ==============================

    @Test fun `createSession minimum`() = runTest {
        client().createSession(agentId = "agent_x", environmentId = "env_x")
    }

    @Test fun `createSession with title`() = runTest {
        client().createSession(
            agentId = "agent_x",
            environmentId = "env_x",
            title = "demo",
        )
    }

    @Test fun `retrieveSession`() = runTest { client().retrieveSession("sess_x") }
    @Test fun `updateSession`()   = runTest { client().updateSession("sess_x") }
    @Test fun `listSessions`()    = runTest { client().listSessions().collect() }

    // ---- Session events ----

    @Test fun `listSessionEvents`()        = runTest { client().listSessionEvents("sess_x").collect() }
    @Test fun `streamSessionEvents`()      = runTest { client().streamSessionEvents("sess_x").collect() }

    @Test fun `sendSessionEvents — every UserEvent variant`() = runTest {
        client().sendSessionEvents(
            sessionId = "sess_x",
            events = listOf(
                UserEvent.Message(text = "hi"),
                UserEvent.Interrupt,
                UserEvent.CustomToolResult(customToolUseId = "t1", text = "ok"),
                UserEvent.ToolConfirmation(toolUseId = "t2", approve = true),
                UserEvent.ToolConfirmation(toolUseId = "t3", approve = false),
                UserEvent.DefineOutcome(
                    description = "succeed",
                    rubric = OutcomeRubric.Text(content = "rules"),
                ),
                UserEvent.DefineOutcome(
                    description = "succeed",
                    rubric = OutcomeRubric.File(fileId = "file_y"),
                ),
            ),
        )
    }

    // ---- Session threads ----

    @Test fun `listSessionThreads`()        = runTest { client().listSessionThreads("sess_x").collect() }
    @Test fun `retrieveSessionThread`()     = runTest { client().retrieveSessionThread("sess_x", "t_x") }
    @Test fun `archiveSessionThread`()      = runTest { client().archiveSessionThread("sess_x", "t_x") }
    @Test fun `listSessionThreadEvents`()   = runTest { client().listSessionThreadEvents("sess_x", "t_x").collect() }
    @Test fun `streamSessionThreadEvents`() = runTest { client().streamSessionThreadEvents("sess_x", "t_x").collect() }

    // ---- Session resources ----

    @Test fun `listSessionResources`()    = runTest { client().listSessionResources("sess_x").collect() }
    @Test fun `retrieveSessionResource`() = runTest { client().retrieveSessionResource("sess_x", "r_x") }
    @Test fun `addSessionFileResource`()  = runTest { client().addSessionFileResource("sess_x", "file_x") }
    @Test fun `updateSessionResource`()   = runTest { client().updateSessionResource("sess_x", "r_x", authorizationToken = "tok") }
    @Test fun `deleteSessionResource`()   = runTest { client().deleteSessionResource("sess_x", "r_x") }

    // ============================== Vaults ==============================

    @Test fun `createVault`()    = runTest { client().createVault(displayName = "main") }
    @Test fun `retrieveVault`()  = runTest { client().retrieveVault("vault_x") }
    @Test fun `updateVault`()    = runTest { client().updateVault("vault_x") }
    @Test fun `deleteVault`()    = runTest { client().deleteVault("vault_x") }
    @Test fun `listVaults`()     = runTest { client().listVaults().collect() }

    // ---- Vault credentials ----

    @Test fun `createVaultCredential McpOAuth`() = runTest {
        client().createVaultCredential(
            vaultId = "vault_x",
            auth = CredentialAuth.McpOAuth(accessToken = "tok", mcpServerUrl = "https://x"),
            displayName = "name",
        )
    }

    @Test fun `createVaultCredential StaticBearer`() = runTest {
        client().createVaultCredential(
            vaultId = "vault_x",
            auth = CredentialAuth.StaticBearer(token = "tok", mcpServerUrl = "https://x"),
        )
    }

    @Test fun `retrieveVaultCredential`()        = runTest { client().retrieveVaultCredential("vault_x", "c_x") }
    @Test fun `archiveVaultCredential`()         = runTest { client().archiveVaultCredential("vault_x", "c_x") }
    @Test fun `deleteVaultCredential`()          = runTest { client().deleteVaultCredential("vault_x", "c_x") }
    @Test fun `listVaultCredentials`()           = runTest { client().listVaultCredentials("vault_x").collect() }
    @Test fun `validateVaultCredentialMcpOAuth`() = runTest {
        client().validateVaultCredentialMcpOAuth("vault_x", "c_x")
    }

    // ============================== MemoryStores ==============================

    @Test fun `createMemoryStore`()   = runTest { client().createMemoryStore(name = "kb", description = "desc") }
    @Test fun `retrieveMemoryStore`() = runTest { client().retrieveMemoryStore("store_x") }
    @Test fun `updateMemoryStore`()   = runTest { client().updateMemoryStore("store_x") }
    @Test fun `listMemoryStores`()    = runTest { client().listMemoryStores().collect() }

    // ---- Memories ----

    @Test fun `createMemory`()   = runTest { client().createMemory("store_x", path = "/a.md", content = "x") }
    @Test fun `retrieveMemory`() = runTest { client().retrieveMemory("store_x", "m_x") }
    @Test fun `updateMemory`()   = runTest { client().updateMemory("store_x", "m_x") }
    @Test fun `deleteMemory`()   = runTest { client().deleteMemory("store_x", "m_x") }
    @Test fun `listMemories`()   = runTest { client().listMemories("store_x").collect() }

    // ---- MemoryVersions ----

    @Test fun `retrieveMemoryVersion`() = runTest { client().retrieveMemoryVersion("store_x", "v_x") }
    @Test fun `redactMemoryVersion`()   = runTest { client().redactMemoryVersion("store_x", "v_x") }
    @Test fun `listMemoryVersions`()    = runTest { client().listMemoryVersions("store_x").collect() }
}
