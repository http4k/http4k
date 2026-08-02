/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.lens

import org.http4k.ai.mcp.model.LogLevel
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.MetaField
import org.http4k.ai.mcp.protocol.ClientCapabilities
import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.util.auto
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiObject
import org.http4k.lens.ParamMeta.ObjectParam

object MetaKey : BiDiLensSpec<Meta, MoshiNode>(
    "meta", ObjectParam,
    LensGet { name, target -> listOfNotNull(target[name]) },
    LensSet { name, values, target ->
        values.fold(target) { acc, next ->
            Meta(MoshiObject((acc.node.attributes + (name to next)).toMutableMap()))
        }
    }
)

inline fun <reified T : Any> MetaKey.progressToken() = auto<T>(MetaField("progressToken"))
fun MetaKey.traceParent() = auto<String>(MetaField("traceparent"))
fun MetaKey.traceState() = auto<String>(MetaField("tracestate"))
fun MetaKey.baggage() = auto<String>(MetaField("baggage"))

// Reserved MCP `_meta` keys (2026-07-28): every stateless request self-describes via these.
private const val MCP = "io.modelcontextprotocol/"
fun MetaKey.protocolVersion() = auto<ProtocolVersion>(MetaField(MCP + "protocolVersion"))
fun MetaKey.clientCapabilities() = auto<ClientCapabilities>(MetaField(MCP + "clientCapabilities"))
fun MetaKey.clientInfo() = auto<VersionedMcpEntity>(MetaField(MCP + "clientInfo"))
fun MetaKey.serverInfo() = auto<VersionedMcpEntity>(MetaField(MCP + "serverInfo"))
fun MetaKey.logLevel() = auto<LogLevel>(MetaField(MCP + "logLevel"))
inline fun <reified T : Any> MetaKey.subscriptionId() = auto<T>(MetaField("io.modelcontextprotocol/subscriptionId"))
