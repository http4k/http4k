/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.conformance.server.resources

import org.http4k.ai.mcp.server.capability.capabilities

/**
 * CapabilityPack containing Resource tests defined in the the MCP Conformance Test Suite
 */
fun ConformanceResources() = capabilities(
    staticTextResource(),
    staticBinaryResource(),
    templateResource(),
    watchedResource(),
    dynamicResource()
)
