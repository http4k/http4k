/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.security

import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.routing.RoutingHttpHandler

object NoMcpSecurity : McpSecurity {
    override val name = "None"
    override val routes = emptyList<RoutingHttpHandler>()
    override val filter = Filter.NoOp
}
