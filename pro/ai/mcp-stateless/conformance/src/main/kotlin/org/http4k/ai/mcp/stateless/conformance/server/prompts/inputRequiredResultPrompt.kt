/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.conformance.server.prompts

import org.http4k.ai.mcp.stateless.ElicitationRequest
import org.http4k.ai.mcp.stateless.ElicitationResponse
import org.http4k.ai.mcp.stateless.PromptResponse
import org.http4k.ai.mcp.stateless.conformance.server.tools.elicitationSchema
import org.http4k.ai.mcp.stateless.model.Content
import org.http4k.ai.mcp.stateless.model.Message
import org.http4k.ai.mcp.stateless.model.Prompt
import org.http4k.ai.model.Role
import org.http4k.routing.stateless.bind

fun inputRequiredResultPrompt() =
    Prompt("test_input_required_result_prompt", "test_input_required_result_prompt") bind { req ->
        when (req.inputResponses["user_context"]) {
            is ElicitationResponse.Ok ->
                PromptResponse.Ok(listOf(Message(Role.User, Content.Text("Context received"))))

            else -> PromptResponse.InputRequired(
                mapOf("user_context" to ElicitationRequest.Form("Provide context", elicitationSchema("context")))
            )
        }
    }
