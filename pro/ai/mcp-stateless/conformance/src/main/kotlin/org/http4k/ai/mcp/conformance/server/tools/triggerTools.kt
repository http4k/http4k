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

// SEP-2575: mutating the live tool list must notify subscribed listen streams with tools/list_changed.
// Reassigning ObservableList.items fires its observers (even with identical content). The supplier resolves
// the chicken-and-egg: the Tools instance is built from a list that includes this tool.
fun testTriggerToolChangeTool(tools: () -> Tools) =
    Tool("test_trigger_tool_change", "test_trigger_tool_change") bind {
        tools().apply { items = items.toList() }

        Ok(textContent)
    }

// As above, for the prompt list -> prompts/list_changed.
fun testTriggerPromptChangeTool(prompts: Prompts) =
    Tool("test_trigger_prompt_change", "test_trigger_prompt_change") bind {
        prompts.apply { items = items.toList() }

        Ok(textContent)
    }
