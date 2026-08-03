/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.protocol.messages.McpSubscriptions
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.protocol.messages.SubscriptionFilter
import org.http4k.ai.mcp.util.McpJson
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.format.MoshiObject
import org.http4k.lens.Header
import org.http4k.lens.XAccelBuffering
import org.http4k.lens.X_ACCEL_BUFFERING
import org.http4k.sse.SseMessage

private const val SUBSCRIPTION_ID = "io.modelcontextprotocol/subscriptionId"

internal fun subscriptionEvent(message: McpJsonRpcRequest) =
    SseMessage.Event("message", McpJson.compact(McpJson.asJsonObject(message)))

internal fun subscriptionIdMeta(id: Any?) = when (id) {
    null -> Meta.default
    else -> Meta(MoshiObject(mutableMapOf(SUBSCRIPTION_ID to McpJson.asJsonObject(id))))
}

internal fun subscriptionSseHeaders() = listOf(
    Header.CONTENT_TYPE.meta.name to TEXT_EVENT_STREAM.withNoDirectives().value,
    Header.X_ACCEL_BUFFERING.meta.name to XAccelBuffering.no.name,
)

internal fun acknowledgement(honored: SubscriptionFilter, id: Any?) =
    McpSubscriptions.Acknowledged.Notification(
        McpSubscriptions.Acknowledged.Notification.Params(honored, subscriptionIdMeta(id))
    )

internal fun toolsListChanged(idMeta: Meta) =
    McpTool.List.Changed.Notification(McpTool.List.Changed.Notification.Params(idMeta))

internal fun promptsListChanged(idMeta: Meta) =
    McpPrompt.List.Changed.Notification(McpPrompt.List.Changed.Notification.Params(idMeta))

internal fun resourcesListChanged(idMeta: Meta) =
    McpResource.List.Changed.Notification(McpResource.List.Changed.Notification.Params(idMeta))

internal fun resourceUpdated(uri: org.http4k.core.Uri, idMeta: Meta) =
    McpResource.Updated.Notification(McpResource.Updated.Notification.Params(uri, idMeta))
