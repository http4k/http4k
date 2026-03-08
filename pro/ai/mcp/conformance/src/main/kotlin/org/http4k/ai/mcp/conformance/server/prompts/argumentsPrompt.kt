/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.conformance.server.prompts

import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.model.Role
import org.http4k.lens.string
import org.http4k.routing.bind

val arg1 = Prompt.Arg.string().required("arg1", "First test argument")
val arg2 = Prompt.Arg.string().required("arg2", "Second test argument")

fun argumentsPrompt() = Prompt(
    "test_prompt_with_arguments",
    "test_prompt_with_arguments",
    arg1,
    arg2,
    title = "A prompt with required arguments"
) bind {
    PromptResponse(
        listOf(
            Message(
                Role.User,
                Content.Text("Prompt with arguments: arg1='${arg1(it)}', arg2='${arg2(it)}")
            )
        )
    )
}
