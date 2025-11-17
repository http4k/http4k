package org.http4k.ai.mcp.conformance.server.tools

import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Tool
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MimeType
import org.http4k.core.ContentType
import org.http4k.routing.bind

val audioContent = Content.Audio(
    Base64Blob.of("UklGRiYAAABXQVZFZm10IBAAAAABAAEAQB8AAAB9AAACABAAZGF0YQIAAAA="),
    MimeType.of(ContentType("audio/wav"))
)

fun audioContentTool() = Tool("test_audio_content", "test_audio_content") bind { Ok(audioContent) }
