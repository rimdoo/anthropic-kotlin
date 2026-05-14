package com.rimdoo.anthropic

import com.anthropic.models.messages.CitationsDelta
import com.anthropic.models.messages.RawContentBlockDelta
import com.anthropic.models.messages.RawContentBlockStartEvent
import com.anthropic.models.messages.RawContentBlockStopEvent
import com.anthropic.models.messages.RawMessageDeltaEvent
import com.anthropic.models.messages.RawMessageStartEvent
import com.anthropic.models.messages.RawMessageStreamEvent

sealed class MessageStreamEvent {
    class MessageStart internal constructor(
        internal val raw: RawMessageStartEvent,
    ) : MessageStreamEvent()

    class ContentBlockStart internal constructor(
        val index: Int,
        internal val raw: RawContentBlockStartEvent,
    ) : MessageStreamEvent()

    data class ContentBlockDelta(
        val index: Int,
        val delta: ContentDelta,
    ) : MessageStreamEvent()

    data class ContentBlockStop(val index: Int) : MessageStreamEvent()

    class MessageDelta internal constructor(
        internal val raw: RawMessageDeltaEvent,
    ) : MessageStreamEvent()

    data object MessageStop : MessageStreamEvent()
}

sealed class ContentDelta {
    data class Text(val text: String) : ContentDelta()
    data class InputJson(val partialJson: String) : ContentDelta()
    data class Thinking(val text: String) : ContentDelta()
    data class Signature(val signature: String) : ContentDelta()
    class Citations internal constructor(
        internal val raw: CitationsDelta,
    ) : ContentDelta()
}

val ContentDelta.text: String
    get() = when (this) {
        is ContentDelta.Text -> text
        else -> ""
    }

internal fun RawMessageStreamEvent.toKotlin(): MessageStreamEvent = when {
    isMessageStart() -> MessageStreamEvent.MessageStart(messageStart().get())
    isContentBlockStart() -> {
        val raw = contentBlockStart().get()
        MessageStreamEvent.ContentBlockStart(index = raw.index().toInt(), raw = raw)
    }
    isContentBlockDelta() -> {
        val raw = contentBlockDelta().get()
        MessageStreamEvent.ContentBlockDelta(
            index = raw.index().toInt(),
            delta = raw.delta().toKotlin(),
        )
    }
    isContentBlockStop() -> {
        val raw = contentBlockStop().get()
        MessageStreamEvent.ContentBlockStop(index = raw.index().toInt())
    }
    isMessageDelta() -> MessageStreamEvent.MessageDelta(messageDelta().get())
    isMessageStop() -> MessageStreamEvent.MessageStop
    else -> error("Unknown RawMessageStreamEvent variant: $this")
}

internal fun RawContentBlockDelta.toKotlin(): ContentDelta = when {
    isText() -> ContentDelta.Text(text().get().text())
    isInputJson() -> ContentDelta.InputJson(inputJson().get().partialJson())
    isThinking() -> ContentDelta.Thinking(thinking().get().thinking())
    isSignature() -> ContentDelta.Signature(signature().get().signature())
    isCitations() -> ContentDelta.Citations(citations().get())
    else -> error("Unknown RawContentBlockDelta variant: $this")
}
