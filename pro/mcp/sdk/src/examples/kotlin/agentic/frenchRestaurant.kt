package agentic

import agentic.tools.restaurant.RestaurantToolPack
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.mcpSse

fun frenchRestaurant() = mcpSse(
    ServerMetaData(McpEntity.of("French Restaurant"), Version.of("0.0.1")),
    RestaurantToolPack("French Restaurant")
)
