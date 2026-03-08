/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package agentic.tools.restaurant

import org.http4k.ai.mcp.server.capability.CapabilityPack

fun RestaurantToolPack(name: String) = CapabilityPack(
    BookMeal(name),
    RestaurantAvailability(name),
)
