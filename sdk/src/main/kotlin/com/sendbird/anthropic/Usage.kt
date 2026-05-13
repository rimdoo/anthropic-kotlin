package com.sendbird.anthropic

import com.anthropic.models.messages.Usage as RawUsage

class Usage internal constructor(
    internal val raw: RawUsage,
) {
    val inputTokens: Long get() = raw.inputTokens()
    val outputTokens: Long get() = raw.outputTokens()
    val cacheReadInputTokens: Long? get() = raw.cacheReadInputTokens().orElse(null)
    val cacheCreationInputTokens: Long? get() = raw.cacheCreationInputTokens().orElse(null)
}
