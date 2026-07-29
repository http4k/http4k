/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.conformance.server.prompts

import org.http4k.ai.mcp.stateless.PromptResponse
import org.http4k.ai.mcp.stateless.model.Content
import org.http4k.ai.mcp.stateless.model.Message
import org.http4k.ai.mcp.stateless.model.Prompt
import org.http4k.ai.mcp.stateless.model.Resource
import org.http4k.ai.model.Role
import org.http4k.connect.model.MimeType
import org.http4k.lens.uri
import org.http4k.routing.stateless.bind

val resourceUri = Prompt.Arg.uri().required("resourceUri", "URI of the resource to embed")

fun embeddedResourcePrompt() = Prompt(
    "test_prompt_with_embedded_resource",
    "test_prompt_with_embedded_resource",
    resourceUri,
    title = "A prompt that includes an embedded resource"
) bind {
    PromptResponse.Ok(
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
