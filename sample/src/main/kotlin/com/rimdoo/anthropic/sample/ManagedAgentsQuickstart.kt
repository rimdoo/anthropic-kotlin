package com.rimdoo.anthropic.sample

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.rimdoo.anthropic.AgentTool
import com.rimdoo.anthropic.NetworkPolicy
import com.rimdoo.anthropic.SessionStreamEvent
import com.rimdoo.anthropic.UserEvent
import com.rimdoo.anthropic.createAgent
import com.rimdoo.anthropic.createEnvironment
import com.rimdoo.anthropic.createSession
import com.rimdoo.anthropic.sendSessionEvents
import com.rimdoo.anthropic.streamSessionEvents
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking

/**
 * Get started with Claude Managed Agents — create agent + environment, start a session,
 * send a user message, stream events.
 *
 * Mirrors the Kotlin/Java translation of the official quickstart at
 * https://platform.claude.com/docs/en/managed-agents/quickstart.
 *
 * Run with:
 *   ANTHROPIC_API_KEY=sk-... ./gradlew :sample:runManagedAgents
 */
fun main(): Unit = runBlocking {
    val client = AnthropicOkHttpClient.fromEnv()

    // 1. Create an agent — model, system prompt, tool access.
    val agent = client.createAgent(
        name = "Coding Assistant",
        model = "claude-opus-4-7",
        system = "You are a helpful coding assistant. Write clean, well-documented code.",
        tools = listOf(AgentTool.Toolset20260401),
    )
    println("Agent ID: ${agent.id}, version: ${agent.version}")

    // 2. Create an environment — a container template the agent runs in.
    val environment = client.createEnvironment(
        name = "quickstart-env",
        networking = NetworkPolicy.Unrestricted,
    )
    println("Environment ID: ${environment.id}")

    // 3. Start a session — a running agent instance for one task.
    val session = client.createSession(
        agentId = agent.id,
        environmentId = environment.id,
        title = "Quickstart session",
    )
    println("Session ID: ${session.id}")

    // 4. Send the user message. The API buffers events until our stream attaches.
    client.sendSessionEvents(
        sessionId = session.id,
        events = listOf(
            UserEvent.Message(
                text = "Create a Python script that generates the first 20 Fibonacci numbers " +
                    "and saves them to fibonacci.txt",
            ),
        ),
    )

    // 5. Stream events until the session goes idle.
    client.streamSessionEvents(session.id)
        .takeWhile { event ->
            when (event) {
                is SessionStreamEvent.AgentMessage -> {
                    print(event.text)
                    true
                }
                is SessionStreamEvent.AgentToolUse -> {
                    println("\n[Using tool: ${event.name}]")
                    true
                }
                is SessionStreamEvent.SessionStatusIdle -> {
                    println("\n\nAgent finished.")
                    false
                }
                else -> true
            }
        }
        .collect { /* side-effects already done in takeWhile */ }
}
