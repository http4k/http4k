/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client

import org.http4k.ai.mcp.protocol.messages.SubscriptionFilter
import org.http4k.core.Uri

internal class SubscriptionSpec {
    internal val toolsHandlers = mutableListOf<() -> Unit>()
    internal val promptsHandlers = mutableListOf<() -> Unit>()
    internal val resourcesHandlers = mutableListOf<() -> Unit>()
    internal val resourceHandlers = mutableMapOf<Uri, MutableList<() -> Unit>>()

    fun onToolsChanged(handler: () -> Unit) = apply { toolsHandlers += handler }
    fun onPromptsChanged(handler: () -> Unit) = apply { promptsHandlers += handler }
    fun onResourcesChanged(handler: () -> Unit) = apply { resourcesHandlers += handler }
    fun onResourceUpdated(uri: Uri, handler: () -> Unit) = apply {
        resourceHandlers.getOrPut(uri) { mutableListOf() } += handler
    }

    internal fun toFilter() = SubscriptionFilter(
        toolsListChanged = toolsHandlers.isNotEmpty().takeIf { it },
        promptsListChanged = promptsHandlers.isNotEmpty().takeIf { it },
        resourcesListChanged = resourcesHandlers.isNotEmpty().takeIf { it },
        resourceSubscriptions = resourceHandlers.keys.map { it.toString() }.ifEmpty { null }
    )
}
