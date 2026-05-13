package com.sendbird.anthropic

import com.anthropic.models.messages.ThinkingConfigDisabled
import com.anthropic.models.messages.ThinkingConfigEnabled
import com.anthropic.models.messages.ThinkingConfigParam

sealed class ThinkingConfig {
    data class Enabled(val budgetTokens: Int) : ThinkingConfig()
    data object Disabled : ThinkingConfig()
}

internal fun ThinkingConfig.toRaw(): ThinkingConfigParam = when (this) {
    is ThinkingConfig.Enabled -> ThinkingConfigParam.ofEnabled(
        ThinkingConfigEnabled.builder().budgetTokens(budgetTokens.toLong()).build()
    )
    is ThinkingConfig.Disabled -> ThinkingConfigParam.ofDisabled(
        ThinkingConfigDisabled.builder().build()
    )
}
