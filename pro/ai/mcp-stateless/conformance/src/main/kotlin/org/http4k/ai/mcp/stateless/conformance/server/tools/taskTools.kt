/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.conformance.server.tools

import org.http4k.ai.mcp.stateless.ElicitationRequest
import org.http4k.ai.mcp.stateless.ElicitationResponse
import org.http4k.ai.mcp.stateless.ToolResponse
import org.http4k.ai.mcp.stateless.model.Tool
import org.http4k.ai.mcp.stateless.server.capability.then
import org.http4k.ai.mcp.stateless.server.extension.McpTasks
import org.http4k.routing.stateless.bind

fun taskTool(tasks: McpTasks) = tasks.asTask()
    .then(Tool("test_task_tool", "test_task_tool") bind { ToolResponse.Ok("task complete") })

fun taskInputRequiredTool(tasks: McpTasks) = tasks.asTask()
    .then(Tool("test_task_input_required_tool", "test_task_input_required_tool") bind { req ->
        when (val answer = req.inputResponses["user_name"]) {
            is ElicitationResponse.Ok -> ToolResponse.Ok("Hello, ${answer.content}")

            else -> ToolResponse.InputRequired(
                mapOf("user_name" to ElicitationRequest.Form("What is your name?", elicitationSchema("name")))
            )
        }
    })
