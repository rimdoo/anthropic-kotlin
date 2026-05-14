package com.rimdoo.anthropic

sealed class CacheControl {
    data object Ephemeral : CacheControl()
}
