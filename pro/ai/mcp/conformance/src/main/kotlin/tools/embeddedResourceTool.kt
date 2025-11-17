package tools

import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.Tool
import org.http4k.connect.model.MimeType.Companion.TEXT_PLAIN
import org.http4k.core.Uri
import org.http4k.routing.bind


val embededResource = Content.EmbeddedResource(
    Resource.Content.Text("This is an embedded resource content.", Uri.of("test://embedded-resource"), TEXT_PLAIN)
)

fun embeddedResourceTool() = Tool("test_embedded_resource", "test_embedded_resource") bind { Ok(embededResource) }

