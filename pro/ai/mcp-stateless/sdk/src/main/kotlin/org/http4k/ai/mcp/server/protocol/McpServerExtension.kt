/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.protocol.McpExtension
import org.http4k.ai.mcp.protocol.McpRpcMethod

/**
 * An extension which the server also serves, rather than merely advertising. Advertising is a wire concern
 * (McpExtension, in core); serving needs the request/response types, which are server-side only - so mpp and
 * x402 declare themselves without implementing this.
 *
 * Note that an extension's messages must still be declared in core: McpJsonRpcRequest is sealed, so an
 * out-of-module extension cannot contribute a parseable wire type.
 */
interface McpServerExtension : McpExtension {
    val methods: Set<McpRpcMethod>
    val filter: McpFilter
}
