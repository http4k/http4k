/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.internal

import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.util.McpJson
import org.http4k.format.MoshiObject
import kotlin.reflect.KClass

internal class McpCallback<T : Any>(
    private val clazz: KClass<T>,
    private val callback: (T, McpMessageId?) -> Unit
) {
    operator fun invoke(req: McpJsonRpcRequest, messageId: McpMessageId?): Boolean =
        try {
            val raw = McpJson.asJsonObject(req) as MoshiObject
            val params = raw["params"] ?: McpJson.nullNode()
            callback(
                McpJson.asA(McpJson.asFormatString(params), clazz),
                messageId
            )
            true
        } catch (e: Exception) {
            false
        }
}
