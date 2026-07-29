/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.conformance.server.tools

import org.http4k.ai.mcp.stateless.ToolResponse.Ok
import org.http4k.ai.mcp.stateless.model.Tool
import org.http4k.routing.stateless.bind

fun multipleContentTypesTool() = Tool("test_multiple_content_types", "test_multiple_content_types") bind {
    Ok(listOf(textContent, imageContent, embededResource))
}
