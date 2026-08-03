/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client

import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.protocol.ClientCapabilities
import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.protocol.messages.mirroredName
import org.http4k.ai.mcp.util.McpJson
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiObject
import org.http4k.lens.Header
import org.http4k.lens.MCP_METHOD
import org.http4k.lens.MCP_NAME
import org.http4k.lens.MCP_PROTOCOL_VERSION
import org.http4k.lens.MetaKey
import org.http4k.lens.clientCapabilities
import org.http4k.lens.clientInfo
import org.http4k.lens.contentType
import org.http4k.lens.protocolVersion

internal fun McpJsonRpcRequest.toHttpRequest(
    protocolVersion: ProtocolVersion,
    endpoint: Uri,
    clientInfo: VersionedMcpEntity,
    capabilities: ClientCapabilities
) = Request(POST, endpoint)
    .contentType(APPLICATION_JSON)
    .with(Header.MCP_PROTOCOL_VERSION of protocolVersion)
    .with(Header.MCP_METHOD of method)
    .let { req -> mirroredName()?.let { req.with(Header.MCP_NAME of it) } ?: req }
    .body(McpJson.compact(McpJson.asJsonObject(this).withClientMeta(protocolVersion, clientInfo, capabilities)))

private fun MoshiNode.withClientMeta(
    protocolVersion: ProtocolVersion,
    clientInfo: VersionedMcpEntity,
    capabilities: ClientCapabilities
): MoshiNode {
    if (this !is MoshiObject) return this
    val params = attributes["params"] as? MoshiObject ?: return this
    val existingMeta = params.attributes["_meta"] as? MoshiObject ?: MoshiObject()
    val meta = MetaKey.protocolVersion().toLens()(
        protocolVersion,
        MetaKey.clientInfo().toLens()(
            clientInfo,
            MetaKey.clientCapabilities().toLens()(capabilities, Meta(existingMeta))
        )
    ).node
    val newParams = MoshiObject((params.attributes + ("_meta" to meta)).toMutableMap())
    return MoshiObject((attributes + ("params" to newParams)).toMutableMap())
}
