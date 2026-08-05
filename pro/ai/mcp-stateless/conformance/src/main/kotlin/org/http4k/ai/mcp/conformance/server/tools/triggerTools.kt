/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.conformance.server.tools

import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.server.protocol.Prompts
import org.http4k.ai.mcp.server.protocol.Tools
import org.http4k.routing.bind

fun testTriggerToolChangeTool(tools: () -> Tools) =
    Tool("test_trigger_tool_change", "test_trigger_tool_change") bind {
        tools().apply { items = items.toList() }

        Ok(textContent)
    }

fun testTriggerPromptChangeTool(prompts: Prompts) =
    Tool("test_trigger_prompt_change", "test_trigger_prompt_change") bind {
        prompts.apply { items = items.toList() }

        Ok(textContent)
    }
