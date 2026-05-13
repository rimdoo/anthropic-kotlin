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
