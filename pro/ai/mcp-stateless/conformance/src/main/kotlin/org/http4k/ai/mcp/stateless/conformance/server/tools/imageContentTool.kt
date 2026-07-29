/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.conformance.server.tools

import org.http4k.ai.mcp.stateless.ToolResponse.Ok
import org.http4k.ai.mcp.stateless.model.Content.Image
import org.http4k.ai.mcp.stateless.model.Tool
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MimeType
import org.http4k.routing.stateless.bind

val imageContent = Image(
    Base64Blob.of("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg=="),
    MimeType.IMAGE_PNG
)

fun imageContentTool() = Tool("test_image_content", "test_image_content") bind { Ok(imageContent) }
