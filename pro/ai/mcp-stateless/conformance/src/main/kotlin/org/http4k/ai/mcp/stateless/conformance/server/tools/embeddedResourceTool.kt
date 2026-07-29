/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.conformance.server.tools

import org.http4k.ai.mcp.stateless.ToolResponse.Ok
import org.http4k.ai.mcp.stateless.model.Content
import org.http4k.ai.mcp.stateless.model.Resource
import org.http4k.ai.mcp.stateless.model.Tool
import org.http4k.connect.model.MimeType.Companion.TEXT_PLAIN
import org.http4k.core.Uri
import org.http4k.routing.stateless.bind

val embededResource = Content.EmbeddedResource(
    Resource.Content.Text("This is an embedded resource content.", Uri.of("test://embedded-resource"), TEXT_PLAIN)
)

fun embeddedResourceTool() = Tool("test_embedded_resource", "test_embedded_resource") bind { Ok(embededResource) }
