package com.rimdoo.anthropic

import com.anthropic.models.messages.ToolUnion
import com.anthropic.models.messages.WebFetchTool20260309
import com.anthropic.models.messages.WebSearchTool20260209

/**
 * Server-side tools that Anthropic itself executes — no client-side handling needed.
 *
 * Unlike custom [Tool] declarations (where the model emits `tool_use` blocks for you to run),
 * a ServerTool's invocation and result are produced by Anthropic and stream back as
 * additional content blocks in the same response.
 *
 * Pass via the `serverTools` parameter on [createMessage] / [streamMessage].
 */
sealed class ServerTool {

    /**
     * Anthropic-hosted web search (`web_search_20260209`).
     *
     * @param maxUses cap on the number of searches the model may issue in one response.
     * @param allowedDomains if non-null, only these domains may appear in results.
     * @param blockedDomains if non-null, these domains are excluded. Mutually exclusive with
     *   [allowedDomains] per the Anthropic spec — supplying both will be rejected server-side.
     */
    data class WebSearch(
        val maxUses: Int? = null,
        val allowedDomains: List<String>? = null,
        val blockedDomains: List<String>? = null,
    ) : ServerTool()

    /**
     * Anthropic-hosted web fetch (`web_fetch_20260309`). Lets the model retrieve a specific
     * URL the user (or an upstream tool) supplied.
     */
    data class WebFetch(
        val maxUses: Int? = null,
        val allowedDomains: List<String>? = null,
        val blockedDomains: List<String>? = null,
    ) : ServerTool()
}

fun ServerTool.toToolUnion(): ToolUnion = when (this) {
    is ServerTool.WebSearch -> ToolUnion.ofWebSearchTool20260209(
        WebSearchTool20260209.builder().apply {
            if (maxUses != null) maxUses(maxUses.toLong())
            if (allowedDomains != null) allowedDomains(allowedDomains)
            if (blockedDomains != null) blockedDomains(blockedDomains)
        }.build()
    )
    is ServerTool.WebFetch -> ToolUnion.ofWebFetchTool20260309(
        WebFetchTool20260309.builder().apply {
            if (maxUses != null) maxUses(maxUses.toLong())
            if (allowedDomains != null) allowedDomains(allowedDomains)
            if (blockedDomains != null) blockedDomains(blockedDomains)
        }.build()
    )
}
