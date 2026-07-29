/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.server

import org.http4k.ai.mcp.stateless.model.Meta
import org.http4k.ai.mcp.stateless.protocol.VersionedMcpEntity
import org.http4k.lens.stateless.MetaKey
import org.http4k.lens.stateless.serverInfo

// Stamps the server identity into a result's `_meta` (io.modelcontextprotocol/serverInfo), at construction time.
fun Meta.withServerInfo(info: VersionedMcpEntity): Meta = MetaKey.serverInfo().toLens()(info, this)
