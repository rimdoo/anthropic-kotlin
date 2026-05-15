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

        /**
         * Bundle multiple tool_result blocks into a single user message.
         *
         * The Messages API requires every tool_use in an assistant turn to be answered in
         * the next user turn. When the model issues several tool_uses in one turn, you
         * must reply with one user message containing all of their results.
         */
        fun toolResults(vararg results: ToolResult): Message = toolResults(results.toList())

        fun toolResults(results: List<ToolResult>): Message = Message(
            MessageParam.builder()
                .role(MessageParam.Role.USER)
                .contentOfBlockParams(
                    results.map { r ->
                        ContentBlockParam.ofToolResult(
                            ToolResultBlockParam.builder()
                                .toolUseId(r.toolUseId)
                                .content(r.content)
                                .apply { if (r.isError) isError(true) }
                                .build()
                        )
                    }
                )
                .build()
        )
    }
}

/** Single tool execution result, paired with the originating [ContentBlock.ToolUse.id]. */
data class ToolResult(
    val toolUseId: String,
    val content: String,
    val isError: Boolean = false,
)
