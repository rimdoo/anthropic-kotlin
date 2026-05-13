@file:JvmName("AnthropicKt")
@file:JvmMultifileClass

package com.sendbird.anthropic

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
): Session = withContext(Dispatchers.IO) {
    try {
        val builder = SessionCreateParams.builder()
            .addBeta(MANAGED_AGENTS)
            .agent(agentId)
        if (environmentId != null) builder.environmentId(environmentId)
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
