package com.sendbird.anthropic

import com.anthropic.models.messages.MessageParam

data class Message internal constructor(
    internal val raw: MessageParam,
) {
    companion object {
        fun user(text: String): Message = Message(
            MessageParam.builder()
                .role(MessageParam.Role.USER)
                .content(text)
                .build()
        )

        fun assistant(text: String): Message = Message(
            MessageParam.builder()
                .role(MessageParam.Role.ASSISTANT)
                .content(text)
                .build()
        )
    }
}
