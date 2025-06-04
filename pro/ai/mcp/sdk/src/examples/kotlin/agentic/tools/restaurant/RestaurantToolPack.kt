package agentic.tools.restaurant

import org.http4k.ai.mcp.server.capability.CapabilityPack

fun RestaurantToolPack(name: String) = CapabilityPack(
    BookMeal(name),
    RestaurantAvailability(name),
)
