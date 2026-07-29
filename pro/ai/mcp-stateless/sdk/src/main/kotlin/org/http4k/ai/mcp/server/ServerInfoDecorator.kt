/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server

import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiObject
import org.http4k.lens.MetaKey
import org.http4k.lens.serverInfo

/**
 * Stamps `io.modelcontextprotocol/serverInfo` into a serialized result's `_meta`, once, at the
 * protocol serialization funnel — rather than in every capability-handler branch. Error and empty
 * responses (no top-level `result`) pass through untouched (JSON-RPC errors carry no `_meta`).
 */
fun MoshiNode.withServerInfo(info: VersionedMcpEntity): MoshiNode {
    if (this !is MoshiObject) return this
    val result = attributes["result"] as? MoshiObject ?: return this
    val meta = result.attributes["_meta"] as? MoshiObject ?: MoshiObject()
    val updatedMeta = MetaKey.serverInfo().toLens()(info, Meta(meta)).node
    val updatedResult = MoshiObject((result.attributes + ("_meta" to updatedMeta)).toMutableMap())
    return MoshiObject((attributes + ("result" to updatedResult)).toMutableMap())
}
