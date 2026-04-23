/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.internal

import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.flatMapFailure
import dev.forkhandles.result4k.map
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.ResourceResponse.Ok
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.protocol.messages.McpRpc
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.core.Uri
import java.time.Duration

internal class ClientResources(
    private val queueFor: (McpMessageId) -> Iterable<McpNodeType>,
    private val tidyUp: (McpMessageId) -> Unit,
    private val defaultTimeout: Duration,
    private val sender: McpRpcSender,
    private val id: () -> McpMessageId,
    private val register: (McpRpc, McpCallback<*>) -> Any
) : McpClient.Resources {

    private val subscriptions = mutableMapOf<Uri, MutableList<() -> Unit>>()

    override fun onChange(fn: () -> Unit) {
        register(McpResource.List, McpCallback(McpResource.List.Changed.Notification.Params::class) { _, _ ->
            fn()
        })
    }

    override fun subscribe(uri: Uri, fn: () -> Unit) {
        register(McpResource.Updated, McpCallback(McpResource.Updated.Notification.Params::class) { notification, _ ->
            subscriptions[notification.uri]?.forEach { it() }
        })
        id().let { messageId ->
            sender(
                McpResource.Subscribe.Request(McpResource.Subscribe.Request.Params(uri), messageId),
                defaultTimeout,
                messageId
            )
        }
        subscriptions.getOrPut(uri, ::mutableListOf).add(fn)
    }

    override fun unsubscribe(uri: Uri) {
        id().let { messageId ->
            sender(
                McpResource.Unsubscribe.Request(McpResource.Unsubscribe.Request.Params(uri), messageId),
                defaultTimeout,
                messageId
            )
        }
        subscriptions -= uri
    }

    override fun list(overrideDefaultTimeout: Duration?) = id().let { messageId ->
        sender(
            McpResource.List.Request(McpResource.List.Request.Params(), messageId),
            overrideDefaultTimeout ?: defaultTimeout,
            messageId
        )
    }
        .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
        .flatMap { it.first().asOrFailure<McpResource.List.Response.Result>() }
        .map { it.resources }

    override fun listTemplates(overrideDefaultTimeout: Duration?) = id().let { messageId ->
        sender(
            McpResource.ListTemplates.Request(McpResource.ListTemplates.Request.Params(), messageId),
            overrideDefaultTimeout ?: defaultTimeout,
            messageId
        )
    }
        .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
        .flatMap { it.first().asOrFailure<McpResource.ListTemplates.Response.Result>() }
        .map { it.resourceTemplates }

    override fun read(request: ResourceRequest, overrideDefaultTimeout: Duration?) =
        id().let { messageId ->
            sender(
                McpResource.Read.Request(McpResource.Read.Request.Params(request.uri, request.meta), messageId),
                overrideDefaultTimeout ?: defaultTimeout,
                messageId
            )
        }
            .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
            .flatMap { it.first().asOrFailure<McpResource.Read.Response.Result>() }
            .map { Ok(it.contents) as ResourceResponse }
            .flatMapFailure { toResourceErrorOrFailure(it) }
}
