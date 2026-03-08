/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.conformance.server.tools

import org.http4k.ai.mcp.server.capability.CapabilityPack

/**
 * CapabilityPack containing Tool tests defined in the the MCP Conformance Test Suite
 */
fun ConformanceTools() = CapabilityPack(
    simpleTextTool(),
    imageContentTool(),
    audioContentTool(),
    embeddedResourceTool(),
    multipleContentTypesTool(),
    progressTool(),
    errorHandlingTool(),
    samplingTool(),
    elicitationTool(),
    elicitationSep1034Tool(),
    elicitationSep1330Enums(),
    dynamicTool(),
    loggingTool()
)
