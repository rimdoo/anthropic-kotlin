package com.sendbird.anthropic

sealed class CacheControl {
    data object Ephemeral : CacheControl()
}
