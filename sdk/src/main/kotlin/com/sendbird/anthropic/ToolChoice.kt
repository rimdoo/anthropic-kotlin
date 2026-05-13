package com.sendbird.anthropic

import com.anthropic.models.messages.ToolChoiceAny
import com.anthropic.models.messages.ToolChoiceAuto
import com.anthropic.models.messages.ToolChoiceNone
import com.anthropic.models.messages.ToolChoice as RawToolChoice
import com.anthropic.models.messages.ToolChoiceTool as RawToolChoiceTool

sealed class ToolChoice {
    /** Let the model decide whether to use a tool. (default) */
    data object Auto : ToolChoice()

    /** Force the model to use any one of the provided tools. */
    data object Any : ToolChoice()

    /** Disable tool use even if tools are declared. */
    data object None : ToolChoice()

    /** Force the model to call this specific tool by name. */
    data class Tool(val name: String) : ToolChoice()
}

internal fun ToolChoice.toRaw(): RawToolChoice = when (this) {
    is ToolChoice.Auto -> RawToolChoice.ofAuto(ToolChoiceAuto.builder().build())
    is ToolChoice.Any -> RawToolChoice.ofAny(ToolChoiceAny.builder().build())
    is ToolChoice.None -> RawToolChoice.ofNone(ToolChoiceNone.builder().build())
    is ToolChoice.Tool -> RawToolChoice.ofTool(RawToolChoiceTool.builder().name(name).build())
}
