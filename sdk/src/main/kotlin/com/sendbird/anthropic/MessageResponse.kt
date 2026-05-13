package com.sendbird.anthropic

import com.anthropic.models.messages.Message as RawMessage

class MessageResponse internal constructor(
    internal val raw: RawMessage,
) {
    val id: String get() = raw.id()
    val content: List<ContentBlock> by lazy { raw.content().map { it.toKotlin() } }
    val usage: Usage by lazy { Usage(raw.usage()) }
}

val MessageResponse.text: String
    get() = content.filterIsInstance<ContentBlock.Text>().firstOrNull()?.text ?: ""
