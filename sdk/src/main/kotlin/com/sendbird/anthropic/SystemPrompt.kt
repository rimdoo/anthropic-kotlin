package com.sendbird.anthropic

import com.anthropic.models.messages.CacheControlEphemeral
import com.anthropic.models.messages.TextBlockParam

class SystemPrompt internal constructor(
    internal val blocks: List<TextBlockParam>,
)

class SystemPromptBuilder internal constructor() {
    private val blocks = mutableListOf<TextBlockParam>()

    fun text(content: String, cache: CacheControl? = null) {
        val b = TextBlockParam.builder().text(content)
        if (cache is CacheControl.Ephemeral) {
            b.cacheControl(CacheControlEphemeral.builder().build())
        }
        blocks.add(b.build())
    }

    internal fun build(): SystemPrompt = SystemPrompt(blocks.toList())
}

fun systemPrompt(build: SystemPromptBuilder.() -> Unit): SystemPrompt =
    SystemPromptBuilder().apply(build).build()
