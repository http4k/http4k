/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.conformance.server.misc

import org.http4k.ai.mcp.stateless.conformance.server.prompts.emptyCompletion
import org.http4k.ai.mcp.stateless.server.capability.completions

/**
 * CapabilityPack containing miscellaneous tests defined in the the MCP Conformance Test Suite
 */
fun ConformanceMisc() = completions(emptyCompletion())
