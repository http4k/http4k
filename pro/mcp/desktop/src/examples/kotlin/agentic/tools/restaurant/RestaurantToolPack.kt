package agentic.tools.restaurant

import org.http4k.mcp.capability.CapabilityPack

fun RestaurantToolPack(name: String) = CapabilityPack(
    BookMeal(name),
    RestaurantAvailability(name),
)
