package com.rimdoo.anthropic

import kotlin.test.Test

/**
 * Verifies every `*.toRaw()` / `.toParam()` / factory function that constructs a
 * Java SDK union member.
 *
 * The Java SDK validates required discriminator fields (e.g. `type`) at `Builder.build()`
 * time — so calling each mapper here exercises that validation without any network I/O.
 * A bug like a missing `.type()` setter shows up as `IllegalStateException` in the
 * relevant test (which is exactly what the user hit at runtime in Apr-2026).
 */
class MappersTest {

    // -------- AgentTool --------
    @Test fun `AgentTool Toolset20260401 builds`() {
        AgentTool.Toolset20260401.toRaw()
    }

    @Test fun `AgentTool Toolset20260401Subset builds for every default tool`() {
        AgentTool.DefaultTool.values().forEach { tool ->
            AgentTool.Toolset20260401Subset(enabled = setOf(tool)).toRaw()
        }
        AgentTool.Toolset20260401Subset(enabled = AgentTool.DefaultTool.values().toSet()).toRaw()
    }

    @Test fun `AgentTool Toolset20260401Subset rejects empty enabled`() {
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            AgentTool.Toolset20260401Subset(enabled = emptySet())
        }
    }

    @Test fun `AgentTool McpToolset builds with all tools`() {
        AgentTool.McpToolset(serverName = "atlassian").toRaw()
    }

    @Test fun `AgentTool McpToolset builds with subset`() {
        AgentTool.McpToolset(
            serverName = "atlassian",
            enabled = setOf("searchConfluenceUsingCql", "getConfluencePage"),
        ).toRaw()
    }

    @Test fun `AgentTool McpToolset rejects empty enabled`() {
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            AgentTool.McpToolset(serverName = "x", enabled = emptySet())
        }
    }

    @Test fun `AgentTool CustomTool builds`() {
        AgentTool.CustomTool(
            name = "lookup_user",
            description = "Look up a user by id.",
            rawInputSchema = com.anthropic.models.beta.agents.BetaManagedAgentsCustomToolInputSchema.builder()
                .type(com.anthropic.models.beta.agents.BetaManagedAgentsCustomToolInputSchema.Type.OBJECT)
                .build(),
        ).toRaw()
    }

    @Test fun `AgentTool Other accepts raw Java union from outside`() {
        val raw = com.anthropic.models.beta.agents.AgentCreateParams.Tool.ofAgentToolset20260401(
            com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolset20260401Params.builder()
                .type(com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolset20260401Params.Type.AGENT_TOOLSET_20260401)
                .build()
        )
        AgentTool.Other(raw).toRaw()
    }

    // -------- McpUrlServer --------
    @Test fun `McpUrlServer builds`() {
        McpUrlServer(name = "atlassian", url = "https://mcp.atlassian.com/v1/sse").toRaw()
    }

    // -------- ServerTool --------
    @Test fun `ServerTool WebSearch builds with every combination`() {
        ServerTool.WebSearch().toToolUnion()
        ServerTool.WebSearch(maxUses = 3).toToolUnion()
        ServerTool.WebSearch(allowedDomains = listOf("anthropic.com")).toToolUnion()
        ServerTool.WebSearch(blockedDomains = listOf("evil.example")).toToolUnion()
    }

    @Test fun `ServerTool WebFetch builds with every combination`() {
        ServerTool.WebFetch().toToolUnion()
        ServerTool.WebFetch(maxUses = 5).toToolUnion()
        ServerTool.WebFetch(allowedDomains = listOf("anthropic.com")).toToolUnion()
        ServerTool.WebFetch(blockedDomains = listOf("evil.example")).toToolUnion()
    }

    // -------- NetworkPolicy --------
    @Test fun `NetworkPolicy Unrestricted builds`() {
        NetworkPolicy.Unrestricted.toRaw()
    }

    // -------- OutcomeRubric --------
    @Test fun `OutcomeRubric Text builds`() {
        OutcomeRubric.Text(content = "Must be Michelin-starred.").toRaw()
    }

    @Test fun `OutcomeRubric File builds`() {
        OutcomeRubric.File(fileId = "file_abc").toRaw()
    }

    // -------- CredentialAuth --------
    @Test fun `CredentialAuth McpOAuth builds`() {
        CredentialAuth.McpOAuth(accessToken = "tok", mcpServerUrl = "https://mcp.example.com").toRaw()
    }

    @Test fun `CredentialAuth StaticBearer builds`() {
        CredentialAuth.StaticBearer(token = "tok", mcpServerUrl = "https://mcp.example.com").toRaw()
    }

    // -------- ThinkingConfig --------
    @Test fun `ThinkingConfig Enabled builds`() {
        ThinkingConfig.Enabled(budgetTokens = 2048).toRaw()
    }

    @Test fun `ThinkingConfig Disabled builds`() {
        ThinkingConfig.Disabled.toRaw()
    }

    // -------- ToolChoice --------
    @Test fun `ToolChoice Auto builds`() {
        ToolChoice.Auto.toRaw()
    }

    @Test fun `ToolChoice Any builds`() {
        ToolChoice.Any.toRaw()
    }

    @Test fun `ToolChoice None builds`() {
        ToolChoice.None.toRaw()
    }

    @Test fun `ToolChoice Tool(name) builds`() {
        ToolChoice.Tool(name = "get_weather").toRaw()
    }

    // -------- UserEvent (all 5 variants) --------
    @Test fun `UserEvent Message builds`() {
        UserEvent.Message(text = "hello").toRaw()
    }

    @Test fun `UserEvent Interrupt builds`() {
        UserEvent.Interrupt.toRaw()
    }

    @Test fun `UserEvent CustomToolResult builds`() {
        UserEvent.CustomToolResult(customToolUseId = "toolu_x", text = "result").toRaw()
    }

    @Test fun `UserEvent ToolConfirmation approve builds`() {
        UserEvent.ToolConfirmation(toolUseId = "toolu_x", approve = true).toRaw()
    }

    @Test fun `UserEvent ToolConfirmation deny builds`() {
        UserEvent.ToolConfirmation(toolUseId = "toolu_x", approve = false).toRaw()
    }

    @Test fun `UserEvent DefineOutcome with Text rubric builds`() {
        UserEvent.DefineOutcome(
            description = "find a 5-star Michelin restaurant",
            rubric = OutcomeRubric.Text(content = "Must be Michelin-starred."),
        ).toRaw()
    }

    @Test fun `UserEvent DefineOutcome with File rubric builds`() {
        UserEvent.DefineOutcome(
            description = "match the uploaded rubric",
            rubric = OutcomeRubric.File(fileId = "file_xyz"),
        ).toRaw()
    }

    // -------- ContentBlock.toParam (all variants) --------
    @Test fun `ContentBlock Text toParam builds`() {
        ContentBlock.Text("hi").toParam()
    }

    @Test fun `ContentBlock Image url toParam builds`() {
        ContentBlock.Image.url("https://example.com/cat.jpg").toParam()
    }

    @Test fun `ContentBlock ToolUse toParam builds`() {
        ContentBlock.ToolUse(
            id = "toolu_abc",
            name = "get_weather",
            input = mapOf("city" to "Seoul"),
        ).toParam()
    }

    @Test fun `ContentBlock Thinking toParam builds`() {
        ContentBlock.Thinking(thinking = "let me think...", signature = "sig_x").toParam()
    }

    // -------- Message factories --------
    @Test fun `Message user String builds`() {
        Message.user("hi")
    }

    @Test fun `Message assistant String builds`() {
        Message.assistant("hi back")
    }

    @Test fun `Message assistant content list builds`() {
        Message.assistant(
            content = listOf(
                ContentBlock.Text("hi"),
                ContentBlock.ToolUse(id = "toolu_x", name = "get_weather", input = emptyMap()),
            ),
        )
    }

    @Test fun `Message toolResult builds`() {
        Message.toolResult(toolUseId = "toolu_x", content = "sunny, 21C")
    }

    @Test fun `Message user vararg builds`() {
        Message.user(
            ContentBlock.Image.url("https://example.com/x.png"),
            ContentBlock.Text("이게 뭐야?"),
        )
    }

    // -------- Tool (lazy raw build) --------
    @Test fun `Tool with jsonSchema lazy raw builds`() {
        val t = Tool(
            name = "get_weather",
            description = "current weather",
            inputSchema = jsonSchema {
                property("city", type = "string", required = true)
            },
        )
        // .raw is internal lazy — accessing forces .build()
        t.raw
    }
}
