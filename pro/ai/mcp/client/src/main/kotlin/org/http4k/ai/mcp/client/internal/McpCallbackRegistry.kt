/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.internal

import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.util.McpJson
import kotlin.reflect.KClass

internal class McpCallbackRegistry {
    private val callbacks = mutableMapOf<KClass<out McpJsonRpcRequest>, MutableList<(McpJsonRpcRequest, McpMessageId?) -> Unit>>()

    @Suppress("UNCHECKED_CAST")
    fun <T : McpJsonRpcRequest> on(clazz: KClass<T>, callback: (T, McpMessageId?) -> Unit) {
        callbacks.getOrPut(clazz) { mutableListOf() }.add { req, id -> callback(req as T, id) }
    }

    fun dispatch(message: McpJsonRpcRequest) {
        val id = message.id?.let { McpJson.asA<McpMessageId>(McpJson.compact(McpJson.asJsonObject(it))) }
        callbacks[message::class]?.forEach { it(message, id) }
    }
}
