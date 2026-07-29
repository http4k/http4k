/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package agentic

import agentic.tools.restaurant.RestaurantToolPack
import org.http4k.ai.mcp.stateless.model.McpEntity
import org.http4k.ai.mcp.stateless.protocol.ServerMetaData
import org.http4k.ai.mcp.stateless.protocol.Version
import org.http4k.ai.mcp.stateless.server.security.NoMcpSecurity
import org.http4k.routing.stateless.mcp

fun frenchRestaurant() = mcp(
    ServerMetaData(McpEntity.of("French Restaurant"), Version.of("0.0.1")),
    NoMcpSecurity,
    RestaurantToolPack("French Restaurant")
)
