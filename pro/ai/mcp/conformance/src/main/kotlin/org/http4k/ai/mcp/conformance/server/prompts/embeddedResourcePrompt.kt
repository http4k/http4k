package org.http4k.ai.mcp.conformance.server.prompts

import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.model.Role
import org.http4k.connect.model.MimeType
import org.http4k.lens.uri
import org.http4k.routing.bind

val resourceUri = Prompt.Arg.uri().required("resourceUri", "URI of the resource to embed")

fun embeddedResourcePrompt() = Prompt(
    "test_prompt_with_embedded_resource",
    "test_prompt_with_embedded_resource",
    resourceUri,
    title = "A prompt that includes an embedded resource"
) bind {
    PromptResponse(
        listOf(
            Message(
                Role.User, Content.EmbeddedResource(
                    Resource.Content.Text(
                        "Embedded resource content for testing", resourceUri(it),
                        MimeType.TEXT_PLAIN
                    )
                )
            ),
            Message(Role.User, Content.Text("Please process the embedded resource above."))
        )
    )
}
