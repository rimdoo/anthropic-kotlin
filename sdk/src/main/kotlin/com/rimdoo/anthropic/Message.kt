package com.rimdoo.anthropic

import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.ToolResultBlockParam

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

        fun user(vararg blocks: ContentBlock): Message = Message(
            MessageParam.builder()
                .role(MessageParam.Role.USER)
                .contentOfBlockParams(blocks.map { it.toParam() })
                .build()
        )

        fun assistant(text: String): Message = Message(
            MessageParam.builder()
                .role(MessageParam.Role.ASSISTANT)
                .content(text)
                .build()
        )

        fun assistant(content: List<ContentBlock>): Message = Message(
            MessageParam.builder()
                .role(MessageParam.Role.ASSISTANT)
                .contentOfBlockParams(content.map { it.toParam() })
                .build()
        )

        fun toolResult(toolUseId: String, content: String): Message = Message(
            MessageParam.builder()
                .role(MessageParam.Role.USER)
                .contentOfBlockParams(
                    listOf(
                        ContentBlockParam.ofToolResult(
                            ToolResultBlockParam.builder()
                                .toolUseId(toolUseId)
                                .content(content)
                                .build()
                        )
                    )
                )
                .build()
        )
    }
}
