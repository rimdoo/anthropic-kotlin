package com.sendbird.anthropic

import com.anthropic.core.JsonValue
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.ImageBlockParam
import com.anthropic.models.messages.TextBlockParam
import com.anthropic.models.messages.ToolUseBlockParam
import com.anthropic.models.messages.UrlImageSource
import com.anthropic.models.messages.ContentBlock as RawContentBlock

sealed class ContentBlock {
    data class Text(val text: String) : ContentBlock()

    data class ToolUse(
        val id: String,
        val name: String,
        val input: Map<String, Any?>,
    ) : ContentBlock()

    /** Request-side image block (URL or base64). Claude does not return Image in responses. */
    class Image internal constructor(
        internal val source: ImageBlockParam.Source,
    ) : ContentBlock() {
        companion object {
            fun url(url: String): Image = Image(
                ImageBlockParam.Source.ofUrl(
                    UrlImageSource.builder().url(url).build()
                )
            )
        }
    }

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

internal fun ContentBlock.toParam(): ContentBlockParam = when (this) {
    is ContentBlock.Text -> ContentBlockParam.ofText(
        TextBlockParam.builder().text(text).build()
    )
    is ContentBlock.ToolUse -> {
        val inputObj = ToolUseBlockParam.Input.builder()
            .additionalProperties(input.mapValues { JsonValue.from(it.value) })
            .build()
        ContentBlockParam.ofToolUse(
            ToolUseBlockParam.builder()
                .id(id)
                .name(name)
                .input(inputObj)
                .build()
        )
    }
    is ContentBlock.Image -> ContentBlockParam.ofImage(
        ImageBlockParam.builder().source(source).build()
    )
    is ContentBlock.Other -> raw.toParam()
}
