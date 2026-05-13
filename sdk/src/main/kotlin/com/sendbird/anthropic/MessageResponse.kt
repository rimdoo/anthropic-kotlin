package com.sendbird.anthropic

import com.anthropic.models.messages.Message as RawMessage

class MessageResponse internal constructor(
    internal val raw: RawMessage,
) {
    val id: String get() = raw.id()
}

val MessageResponse.text: String
    get() = raw.content()
        .firstOrNull { it.isText() }
        ?.asText()
        ?.text()
        ?: ""
