/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.conformance.server.misc

import org.http4k.ai.mcp.conformance.server.prompts.emptyCompletion
import org.http4k.ai.mcp.server.capability.capabilities

/**
 * CapabilityPack containing miscellaneous tests defined in the the MCP Conformance Test Suite
 */
fun ConformanceMisc() = capabilities(emptyCompletion())
