/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.conformance.server.prompts

import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.conformance.server.tools.imageContent
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.model.Role
import org.http4k.connect.model.MimeType
import org.http4k.routing.bind

fun imagePrompt() = Prompt("test_prompt_with_image", "test_prompt_with_image", title = "Prompt With Image") bind {
    PromptResponse(
        listOf(
            Message(Role.User, Content.Image(imageContent.data, MimeType.IMAGE_PNG)),
            Message(Role.User, Content.Text("Please analyze the image above."))
        )
    )
}
