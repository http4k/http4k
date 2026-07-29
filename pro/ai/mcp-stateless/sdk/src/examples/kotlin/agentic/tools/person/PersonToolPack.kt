/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package agentic.tools.person

import org.http4k.ai.mcp.server.capability.CapabilityPack
import java.time.LocalDate
import java.time.YearMonth

fun PersonToolPack(name: String, appointments: (YearMonth) -> Map<LocalDate, List<String>>) = CapabilityPack(
    Diary(name, appointments),
)
