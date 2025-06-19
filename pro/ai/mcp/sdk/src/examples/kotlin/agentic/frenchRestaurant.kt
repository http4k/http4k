package agentic

import agentic.tools.restaurant.RestaurantToolPack
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.routing.mcpHttpStreaming

fun frenchRestaurant() = mcpHttpStreaming(
    ServerMetaData(McpEntity.of("French Restaurant"), Version.of("0.0.1")),
    NoMcpSecurity,
    RestaurantToolPack("French Restaurant")
)
