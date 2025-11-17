package org.http4k.ai.mcp.conformance.server.tools

import org.http4k.ai.mcp.ToolResponse.*
import org.http4k.ai.mcp.model.Content.Image
import org.http4k.ai.mcp.model.Tool
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MimeType
import org.http4k.routing.bind

val imageContent = Image(
    Base64Blob.of("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg=="),
    MimeType.IMAGE_PNG
)

fun imageContentTool() = Tool("test_image_content", "test_image_content") bind { Ok(imageContent) }
