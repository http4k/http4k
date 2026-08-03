/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.conformance.server.tools

import org.http4k.ai.mcp.server.capability.tools
import org.http4k.ai.mcp.server.protocol.Prompts
import org.http4k.ai.mcp.server.protocol.Tools

/**
 * CapabilityPack containing Tool tests defined in the the MCP Conformance Test Suite.
 * Takes the live [Prompts] so the trigger tools can mutate it (prompts/list_changed), and captures its own
 * [Tools] instance so test_trigger_tool_change can mutate the tool list (tools/list_changed).
 */
fun ConformanceTools(prompts: Prompts): Tools {
    lateinit var self: Tools
    val mutableToolList = mutableListOf(
        simpleTextTool(),
        imageContentTool(),
        audioContentTool(),
        embeddedResourceTool(),
        multipleContentTypesTool(),
        progressTool(),
        errorHandlingTool(),
        dynamicTool(),
        loggingTool(),
        inputRequiredResultElicitationTool(),
        inputRequiredResultRequestStateTool(),
        inputRequiredResultMultiRoundTool(),
        inputRequiredResultCapabilitiesTool(),
        inputRequiredResultTamperedStateTool(),
        testMissingCapabilityTool(),
        testStreamingElicitationTool(),
        testLoggingTool(),
        testTriggerToolChangeTool { self },
        testTriggerPromptChangeTool(prompts)
    )
    self = tools(mutableToolList)
    return self
}
