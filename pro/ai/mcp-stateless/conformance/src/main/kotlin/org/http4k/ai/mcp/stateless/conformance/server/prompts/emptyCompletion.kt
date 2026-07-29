/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.conformance.server.prompts

import org.http4k.ai.mcp.stateless.CompletionResponse
import org.http4k.ai.mcp.stateless.model.Reference
import org.http4k.routing.stateless.bind

fun emptyCompletion() = Reference.Prompt("test_prompt_with_arguments") bind {
    CompletionResponse.Ok(listOf())
}
