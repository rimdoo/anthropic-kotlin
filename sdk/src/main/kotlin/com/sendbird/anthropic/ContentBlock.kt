package com.sendbird.anthropic

import com.anthropic.models.messages.ContentBlock as RawContentBlock

sealed class ContentBlock {
    data class Text(val text: String) : ContentBlock()

    data class ToolUse(
        val id: String,
        val name: String,
        val input: Map<String, Any?>,
    ) : ContentBlock()

    /** Escape hatch for response blocks we haven't modeled yet (Thinking, ServerToolUse, etc.). */
    class Other internal constructor(
        internal val raw: RawContentBlock,
    ) : ContentBlock()
}

@Suppress("UNCHECKED_CAST")
internal fun RawContentBlock.toKotlin(): ContentBlock = when {
    isText() -> ContentBlock.Text(asText().text())
    isToolUse() -> {
        val tu = asToolUse()
        ContentBlock.ToolUse(
            id = tu.id(),
            name = tu.name(),
            input = tu._input().convert(Map::class.java) as? Map<String, Any?> ?: emptyMap(),
        )
    }
    else -> ContentBlock.Other(this)
}
