/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.testing.capabilities

import dev.forkhandles.result4k.flatMapFailure
import dev.forkhandles.result4k.map
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.client.internal.toResourceErrorOrFailure
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.testing.TestMcpSender
import org.http4k.ai.mcp.testing.nextEvent
import org.http4k.ai.mcp.testing.toMessage
import org.http4k.core.Uri
import java.time.Duration

class TestingResources(
    private val sender: TestMcpSender
) : McpClient.Resources {
    private val changeNotifications = mutableListOf<() -> Unit>()

    private val subscriptions = mutableMapOf<Uri, MutableList<() -> Unit>>()

    override fun onChange(fn: () -> Unit) {
        changeNotifications += fn
    }

    /**
     * Expect a resource list notification to be made and process it
     */
    fun expectNotification() =
        sender.lastEvent()
            .toMessage<McpResource.List.Changed.Notification>().params
            .also { changeNotifications.forEach { it() } }

    /**
     * Expect a resource updated notification to be made and process it
     */
    fun expectSubscriptionNotification(uri: Uri) =
        sender.lastEvent()
            .toMessage<McpResource.Updated.Notification>().params
            .also {
                require(it.uri == uri) { "Expected notification for $uri, but got ${it.uri}" }
                subscriptions[it.uri]?.forEach { it() }
            }

    override fun list(overrideDefaultTimeout: Duration?) =
        sender(McpResource.List.Request(McpResource.List.Request.Params(), sender.nextId())).first()
            .nextEvent<List<McpResource>, McpResource.List.Response.Result> {
                 resources
            }.map { it.second }

    override fun listTemplates(overrideDefaultTimeout: Duration?) =
        sender(McpResource.ListTemplates.Request(McpResource.ListTemplates.Request.Params(), sender.nextId())).first()
            .nextEvent<List<McpResource>, McpResource.ListTemplates.Response.Result> {
                 resourceTemplates
            }.map { it.second }

    override fun read(request: ResourceRequest, overrideDefaultTimeout: Duration?) =
        sender(McpResource.Read.Request(McpResource.Read.Request.Params(request.uri, request.meta), sender.nextId())).first()
            .nextEvent<ResourceResponse, McpResource.Read.Response.Result>( {
                ResourceResponse.Ok(contents)
            })
            .map { it.second }
            .flatMapFailure { toResourceErrorOrFailure(it) }


    override fun subscribe(uri: Uri, fn: () -> Unit) {
        sender(McpResource.Subscribe.Request(McpResource.Subscribe.Request.Params(uri), sender.nextId()))
        subscriptions.getOrPut(uri, ::mutableListOf).add(fn)
    }

    override fun unsubscribe(uri: Uri) {
        sender(McpResource.Unsubscribe.Request(McpResource.Unsubscribe.Request.Params(uri), sender.nextId()))
        subscriptions -= uri
    }
}
