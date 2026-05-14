@file:JvmName("AnthropicKt")
@file:JvmMultifileClass

package com.rimdoo.anthropic

import com.anthropic.client.AnthropicClient
import com.anthropic.errors.AnthropicException as RawAnthropicException
import com.anthropic.models.beta.AnthropicBeta
import com.anthropic.models.beta.sessions.BetaManagedAgentsSession
import com.anthropic.models.beta.sessions.SessionCreateParams
import com.anthropic.models.beta.sessions.SessionListParams
import com.anthropic.models.beta.sessions.SessionRetrieveParams
import com.anthropic.models.beta.sessions.SessionUpdateParams
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

private val MANAGED_AGENTS = AnthropicBeta.MANAGED_AGENTS_2026_04_01

class Session internal constructor(
    internal val raw: BetaManagedAgentsSession,
) {
    val id: String get() = raw.id()
    val environmentId: String get() = raw.environmentId()
    val createdAt: OffsetDateTime get() = raw.createdAt()
    val archivedAt: OffsetDateTime? get() = raw.archivedAt().orElse(null)
}

suspend fun AnthropicClient.createSession(
    agentId: String,
    environmentId: String? = null,
    title: String? = null,
): Session = withContext(Dispatchers.IO) {
    try {
        val builder = SessionCreateParams.builder()
            .addBeta(MANAGED_AGENTS)
            .agent(agentId)
        if (environmentId != null) builder.environmentId(environmentId)
        if (title != null) builder.title(title)
        Session(beta().sessions().create(builder.build()))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

suspend fun AnthropicClient.retrieveSession(sessionId: String): Session = withContext(Dispatchers.IO) {
    try {
        val params = SessionRetrieveParams.builder()
            .sessionId(sessionId)
            .addBeta(MANAGED_AGENTS)
            .build()
        Session(beta().sessions().retrieve(params))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

suspend fun AnthropicClient.updateSession(sessionId: String): Session = withContext(Dispatchers.IO) {
    try {
        val params = SessionUpdateParams.builder()
            .sessionId(sessionId)
            .addBeta(MANAGED_AGENTS)
            .build()
        Session(beta().sessions().update(params))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

fun AnthropicClient.listSessions(): Flow<Session> = flow {
    try {
        val params = SessionListParams.builder().addBeta(MANAGED_AGENTS).build()
        var page = beta().sessions().list(params)
        while (true) {
            page.data().forEach { emit(Session(it)) }
            if (!page.hasNextPage()) break
            page = page.nextPage()
        }
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}.flowOn(Dispatchers.IO)

// -------- Session events / threads / resources sub-services --------

class SessionEvent internal constructor(
    internal val raw: com.anthropic.models.beta.sessions.events.BetaManagedAgentsSessionEvent,
)

class SessionThread internal constructor(
    internal val raw: com.anthropic.models.beta.sessions.threads.BetaManagedAgentsSessionThread,
) {
    val id: String get() = raw.id()
    val createdAt: OffsetDateTime get() = raw.createdAt()
    val archivedAt: OffsetDateTime? get() = raw.archivedAt().orElse(null)
    val parentThreadId: String? get() = raw.parentThreadId().orElse(null)
}

class SessionResource internal constructor(
    internal val raw: com.anthropic.models.beta.sessions.resources.BetaManagedAgentsSessionResource,
)

fun AnthropicClient.listSessionEvents(sessionId: String): Flow<SessionEvent> = flow {
    try {
        val params = com.anthropic.models.beta.sessions.events.EventListParams.builder()
            .sessionId(sessionId)
            .addBeta(MANAGED_AGENTS)
            .build()
        var page = beta().sessions().events().list(params)
        while (true) {
            page.data().forEach { emit(SessionEvent(it)) }
            if (!page.hasNextPage()) break
            page = page.nextPage()
        }
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}.flowOn(Dispatchers.IO)

fun AnthropicClient.listSessionThreads(sessionId: String): Flow<SessionThread> = flow {
    try {
        val params = com.anthropic.models.beta.sessions.threads.ThreadListParams.builder()
            .sessionId(sessionId)
            .addBeta(MANAGED_AGENTS)
            .build()
        var page = beta().sessions().threads().list(params)
        while (true) {
            page.data().forEach { emit(SessionThread(it)) }
            if (!page.hasNextPage()) break
            page = page.nextPage()
        }
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}.flowOn(Dispatchers.IO)

suspend fun AnthropicClient.retrieveSessionThread(
    sessionId: String,
    threadId: String,
): SessionThread = withContext(Dispatchers.IO) {
    try {
        val params = com.anthropic.models.beta.sessions.threads.ThreadRetrieveParams.builder()
            .sessionId(sessionId)
            .threadId(threadId)
            .addBeta(MANAGED_AGENTS)
            .build()
        SessionThread(beta().sessions().threads().retrieve(params))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

suspend fun AnthropicClient.archiveSessionThread(
    sessionId: String,
    threadId: String,
): SessionThread = withContext(Dispatchers.IO) {
    try {
        val params = com.anthropic.models.beta.sessions.threads.ThreadArchiveParams.builder()
            .sessionId(sessionId)
            .threadId(threadId)
            .addBeta(MANAGED_AGENTS)
            .build()
        SessionThread(beta().sessions().threads().archive(params))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

fun AnthropicClient.listSessionResources(sessionId: String): Flow<SessionResource> = flow {
    try {
        val params = com.anthropic.models.beta.sessions.resources.ResourceListParams.builder()
            .sessionId(sessionId)
            .addBeta(MANAGED_AGENTS)
            .build()
        var page = beta().sessions().resources().list(params)
        while (true) {
            page.data().forEach { emit(SessionResource(it)) }
            if (!page.hasNextPage()) break
            page = page.nextPage()
        }
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}.flowOn(Dispatchers.IO)

// Note: retrieveSessionResource is intentionally omitted — the Java SDK returns
// a ResourceRetrieveResponse union that requires a separate union mapper.
// Use listSessionResources to enumerate.

suspend fun AnthropicClient.deleteSessionResource(
    sessionId: String,
    resourceId: String,
) = withContext(Dispatchers.IO) {
    try {
        val params = com.anthropic.models.beta.sessions.resources.ResourceDeleteParams.builder()
            .sessionId(sessionId)
            .resourceId(resourceId)
            .addBeta(MANAGED_AGENTS)
            .build()
        beta().sessions().resources().delete(params)
        Unit
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

// -------- Session.events.streamStreaming + Session.threads.events --------

/**
 * One event from the session-level event stream. The Java SDK exposes ~30 variants;
 * the ones below are the most commonly handled, and anything else is wrapped in [Other]
 * with the raw payload accessible internally.
 */
sealed class SessionStreamEvent {
    /** Agent emitted assistant message text. Multiple TextBlocks are joined. */
    data class AgentMessage(val text: String) : SessionStreamEvent()

    /** Agent decided to call a built-in tool. */
    data class AgentToolUse(val id: String, val name: String) : SessionStreamEvent()

    /** Built-in tool finished executing. */
    data class AgentToolResult(val toolUseId: String, val isError: Boolean) : SessionStreamEvent()

    /** Agent emitted an extended-thinking block (text is opaque — server-controlled). */
    class AgentThinking internal constructor(
        internal val raw: com.anthropic.models.beta.sessions.events.BetaManagedAgentsAgentThinkingEvent,
    ) : SessionStreamEvent()

    data object SessionStatusRunning : SessionStreamEvent()
    data object SessionStatusIdle : SessionStreamEvent()
    data object SessionStatusTerminated : SessionStreamEvent()

    /** Session encountered an error. The raw event has the error detail. */
    class SessionError internal constructor(
        internal val raw: com.anthropic.models.beta.sessions.events.BetaManagedAgentsSessionErrorEvent,
    ) : SessionStreamEvent()

    /** Escape hatch for variants we haven't modeled (thread events, spans, mcp tool events, etc.). */
    class Other internal constructor(
        internal val raw: com.anthropic.models.beta.sessions.events.BetaManagedAgentsStreamSessionEvents,
    ) : SessionStreamEvent()
}

internal fun com.anthropic.models.beta.sessions.events.BetaManagedAgentsStreamSessionEvents.toKotlin(): SessionStreamEvent = when {
    isAgentMessage() -> SessionStreamEvent.AgentMessage(
        text = asAgentMessage().content().joinToString("") { it.text() }
    )
    isAgentToolUse() -> {
        val raw = asAgentToolUse()
        SessionStreamEvent.AgentToolUse(id = raw.id(), name = raw.name())
    }
    isAgentToolResult() -> {
        val raw = asAgentToolResult()
        SessionStreamEvent.AgentToolResult(
            toolUseId = raw.toolUseId(),
            isError = raw.isError().orElse(false),
        )
    }
    isAgentThinking() -> SessionStreamEvent.AgentThinking(asAgentThinking())
    isSessionStatusRunning() -> SessionStreamEvent.SessionStatusRunning
    isSessionStatusIdle() -> SessionStreamEvent.SessionStatusIdle
    isSessionStatusTerminated() -> SessionStreamEvent.SessionStatusTerminated
    isSessionError() -> SessionStreamEvent.SessionError(asSessionError())
    else -> SessionStreamEvent.Other(this)
}

class SessionThreadStreamEvent internal constructor(
    internal val raw: com.anthropic.models.beta.sessions.threads.BetaManagedAgentsStreamSessionThreadEvents,
)

fun AnthropicClient.streamSessionEvents(sessionId: String): Flow<SessionStreamEvent> = flow {
    try {
        val params = com.anthropic.models.beta.sessions.events.EventStreamParams.builder()
            .sessionId(sessionId)
            .addBeta(MANAGED_AGENTS)
            .build()
        beta().sessions().events().streamStreaming(params).use { stream ->
            val iter = stream.stream().iterator()
            while (iter.hasNext()) {
                emit(iter.next().toKotlin())
            }
        }
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}.flowOn(Dispatchers.IO)

fun AnthropicClient.listSessionThreadEvents(
    sessionId: String,
    threadId: String,
): Flow<SessionEvent> = flow {
    try {
        val params = com.anthropic.models.beta.sessions.threads.events.EventListParams.builder()
            .sessionId(sessionId)
            .threadId(threadId)
            .addBeta(MANAGED_AGENTS)
            .build()
        var page = beta().sessions().threads().events().list(params)
        while (true) {
            page.data().forEach { emit(SessionEvent(it)) }
            if (!page.hasNextPage()) break
            page = page.nextPage()
        }
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}.flowOn(Dispatchers.IO)

fun AnthropicClient.streamSessionThreadEvents(
    sessionId: String,
    threadId: String,
): Flow<SessionThreadStreamEvent> = flow {
    try {
        val params = com.anthropic.models.beta.sessions.threads.events.EventStreamParams.builder()
            .sessionId(sessionId)
            .threadId(threadId)
            .addBeta(MANAGED_AGENTS)
            .build()
        beta().sessions().threads().events().streamStreaming(params).use { stream ->
            val iter = stream.stream().iterator()
            while (iter.hasNext()) {
                emit(SessionThreadStreamEvent(iter.next()))
            }
        }
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}.flowOn(Dispatchers.IO)

// -------- Session.events.send (sealed UserEvent + sendSessionEvents) --------

sealed class UserEvent {
    /** Send a free-form text message from the user. */
    data class Message(val text: String) : UserEvent()
    /** Interrupt the agent's current action. */
    data object Interrupt : UserEvent()
    /** Reply to a custom tool call with a result. */
    data class CustomToolResult(val customToolUseId: String, val text: String) : UserEvent()
    /** Approve or deny an agent's tool call by id. */
    data class ToolConfirmation(val toolUseId: String, val approve: Boolean) : UserEvent()
    /** Declare the success rubric the agent should aim for in this session. */
    data class DefineOutcome(val description: String, val rubric: OutcomeRubric) : UserEvent()
}

sealed class OutcomeRubric {
    /** Inline text rubric. */
    data class Text(val content: String) : OutcomeRubric()
    /** Reference a previously-uploaded file (beta files-api file id) as the rubric. */
    data class File(val fileId: String) : OutcomeRubric()
}

internal fun OutcomeRubric.toRaw(): com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserDefineOutcomeEventParams.Rubric = when (this) {
    is OutcomeRubric.Text -> com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserDefineOutcomeEventParams.Rubric.ofText(
        com.anthropic.models.beta.sessions.events.BetaManagedAgentsTextRubricParams.builder()
            .content(content)
            .build()
    )
    is OutcomeRubric.File -> com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserDefineOutcomeEventParams.Rubric.ofFile(
        com.anthropic.models.beta.sessions.events.BetaManagedAgentsFileRubricParams.builder()
            .fileId(fileId)
            .build()
    )
}

suspend fun AnthropicClient.sendSessionEvents(
    sessionId: String,
    events: List<UserEvent>,
) = withContext(Dispatchers.IO) {
    try {
        val builder = com.anthropic.models.beta.sessions.events.EventSendParams.builder()
            .sessionId(sessionId)
            .addBeta(MANAGED_AGENTS)
        events.forEach { ev ->
            when (ev) {
                is UserEvent.Message -> builder.addEvent(
                    com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserMessageEventParams.builder()
                        .addContent(
                            com.anthropic.models.beta.sessions.events.BetaManagedAgentsTextBlock.builder()
                                .text(ev.text).build()
                        )
                        .build()
                )
                is UserEvent.Interrupt -> builder.addEvent(
                    com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserInterruptEventParams.builder().build()
                )
                is UserEvent.CustomToolResult -> builder.addEvent(
                    com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserCustomToolResultEventParams.builder()
                        .customToolUseId(ev.customToolUseId)
                        .addContent(
                            com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserCustomToolResultEventParams.Content.ofText(
                                com.anthropic.models.beta.sessions.events.BetaManagedAgentsTextBlock.builder()
                                    .text(ev.text).build()
                            )
                        )
                        .build()
                )
                is UserEvent.ToolConfirmation -> builder.addEvent(
                    com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserToolConfirmationEventParams.builder()
                        .toolUseId(ev.toolUseId)
                        .result(
                            if (ev.approve) com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserToolConfirmationEventParams.Result.ALLOW
                            else com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserToolConfirmationEventParams.Result.DENY
                        )
                        .build()
                )
                is UserEvent.DefineOutcome -> builder.addEvent(
                    com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserDefineOutcomeEventParams.builder()
                        .description(ev.description)
                        .rubric(ev.rubric.toRaw())
                        .build()
                )
            }
        }
        beta().sessions().events().send(builder.build())
        Unit
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

// -------- Session.resources sub-service (retrieve / add / update) --------

class SessionResourceDetail internal constructor(
    internal val raw: com.anthropic.models.beta.sessions.resources.ResourceRetrieveResponse,
) {
    val isFile: Boolean get() = raw.isFile()
    val isGitHubRepository: Boolean get() = raw.isGitHubRepository()
    val isMemoryStore: Boolean get() = raw.isMemoryStore()
}

suspend fun AnthropicClient.retrieveSessionResource(
    sessionId: String,
    resourceId: String,
): SessionResourceDetail = withContext(Dispatchers.IO) {
    try {
        val params = com.anthropic.models.beta.sessions.resources.ResourceRetrieveParams.builder()
            .sessionId(sessionId)
            .resourceId(resourceId)
            .addBeta(MANAGED_AGENTS)
            .build()
        SessionResourceDetail(beta().sessions().resources().retrieve(params))
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

/** Attach a previously-uploaded file (by beta files-api file ID) to the session. */
suspend fun AnthropicClient.addSessionFileResource(
    sessionId: String,
    fileId: String,
) = withContext(Dispatchers.IO) {
    try {
        val params = com.anthropic.models.beta.sessions.resources.ResourceAddParams.builder()
            .sessionId(sessionId)
            .fileBetaManagedAgentsFileResourceParams(fileId)
            .addBeta(MANAGED_AGENTS)
            .build()
        beta().sessions().resources().add(params)
        Unit
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}

/** Refresh a resource's authorization token (e.g. rotated OAuth). */
suspend fun AnthropicClient.updateSessionResource(
    sessionId: String,
    resourceId: String,
    authorizationToken: String,
) = withContext(Dispatchers.IO) {
    try {
        val body = com.anthropic.models.beta.sessions.resources.ResourceUpdateParams.Body.builder()
            .authorizationToken(authorizationToken)
            .build()
        val params = com.anthropic.models.beta.sessions.resources.ResourceUpdateParams.builder()
            .sessionId(sessionId)
            .resourceId(resourceId)
            .body(body)
            .addBeta(MANAGED_AGENTS)
            .build()
        beta().sessions().resources().update(params)
        Unit
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}
