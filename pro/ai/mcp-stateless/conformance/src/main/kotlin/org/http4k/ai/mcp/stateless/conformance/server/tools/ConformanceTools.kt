/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.conformance.server.tools

import org.http4k.ai.mcp.stateless.server.capability.tools
import org.http4k.ai.mcp.stateless.server.extension.McpTasks
import org.http4k.ai.mcp.stateless.server.protocol.Prompts
import org.http4k.ai.mcp.stateless.server.protocol.Tools

fun ConformanceTools(prompts: Prompts, tasks: McpTasks): Tools {
    lateinit var self: Tools
    val mutableToolList = mutableListOf(
        simpleTextTool(),
        customHeaderTool(),
        imageContentTool(),
        audioContentTool(),
        embeddedResourceTool(),
        multipleContentTypesTool(),
        progressTool(),
        errorHandlingTool(),
        loggingTool(),
        inputRequiredResultElicitationTool(),
        inputRequiredResultRequestStateTool(),
        inputRequiredResultMultiRoundTool(),
        inputRequiredResultCapabilitiesTool(),
        inputRequiredResultTamperedStateTool(),
        missingCapabilityTool(),
        testStreamingElicitationTool(),
        testLoggingTool(),
        testTriggerToolChangeTool { self },
        testTriggerPromptChangeTool(prompts),
        taskTool(tasks),
        taskInputRequiredTool(tasks)
    )
    self = tools(mutableToolList)
    return self
}
