/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.conformance.server.prompts

import org.http4k.ai.mcp.ElicitationRequest
import org.http4k.ai.mcp.ElicitationResponse
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.conformance.server.tools.elicitationSchema
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.model.Role
import org.http4k.routing.bind

// SEP-2322 MRTR on a non-tool method: prompts/get returns input_required, then completes on the retry.
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
