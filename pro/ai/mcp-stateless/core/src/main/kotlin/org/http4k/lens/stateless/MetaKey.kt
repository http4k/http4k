/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.lens.stateless

import org.http4k.ai.mcp.stateless.model.LogLevel
import org.http4k.ai.mcp.stateless.model.Meta
import org.http4k.ai.mcp.stateless.model.MetaField
import org.http4k.ai.mcp.stateless.protocol.ClientCapabilities
import org.http4k.ai.mcp.stateless.protocol.ProtocolVersion
import org.http4k.ai.mcp.stateless.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.stateless.util.auto
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiObject
import org.http4k.lens.BiDiLensSpec
import org.http4k.lens.LensGet
import org.http4k.lens.LensSet
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
fun MetaKey.traceParent() = MetaKey.auto<String>(MetaField("traceparent"))
fun MetaKey.traceState() = MetaKey.auto<String>(MetaField("tracestate"))
fun MetaKey.baggage() = MetaKey.auto<String>(MetaField("baggage"))

fun MetaKey.protocolVersion() = MetaKey.auto<ProtocolVersion>(MetaField("io.modelcontextprotocol/protocolVersion"))
fun MetaKey.clientCapabilities() = MetaKey.auto<ClientCapabilities>(MetaField("io.modelcontextprotocol/clientCapabilities"))
fun MetaKey.clientInfo() = MetaKey.auto<VersionedMcpEntity>(MetaField("io.modelcontextprotocol/clientInfo"))
fun MetaKey.serverInfo() = MetaKey.auto<VersionedMcpEntity>(MetaField("io.modelcontextprotocol/serverInfo"))
fun MetaKey.logLevel() = MetaKey.auto<LogLevel>(MetaField("io.modelcontextprotocol/logLevel"))

