/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.client.internal

import org.http4k.ai.mcp.stateless.model.McpUri
import org.http4k.ai.mcp.stateless.protocol.messages.SubscriptionFilter

internal class SubscriptionSpec {
    internal val toolsHandlers = mutableListOf<() -> Unit>()
    internal val promptsHandlers = mutableListOf<() -> Unit>()
    internal val resourcesHandlers = mutableListOf<() -> Unit>()
    internal val resourceHandlers = mutableMapOf<McpUri, MutableList<() -> Unit>>()

    fun onToolsChanged(handler: () -> Unit) = apply { toolsHandlers += handler }
    fun onPromptsChanged(handler: () -> Unit) = apply { promptsHandlers += handler }
    fun onResourcesChanged(handler: () -> Unit) = apply { resourcesHandlers += handler }
    fun onResourceUpdated(uri: McpUri, handler: () -> Unit) = apply {
        resourceHandlers.getOrPut(uri) { mutableListOf() } += handler
    }

    internal fun toFilter() = SubscriptionFilter(
        toolsListChanged = toolsHandlers.isNotEmpty().takeIf { it },
        promptsListChanged = promptsHandlers.isNotEmpty().takeIf { it },
        resourcesListChanged = resourcesHandlers.isNotEmpty().takeIf { it },
        resourceSubscriptions = resourceHandlers.keys.map { it.value }.ifEmpty { null }
    )
}
