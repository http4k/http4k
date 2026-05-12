/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.a2a.capabilities

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import org.http4k.ai.a2a.client.A2AClient
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.mcp.ToolResponse.Error
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.lens.with

fun CancelTask(clientFor: (Request) -> A2AClient) = ToolCapability(
    Tool(
        "cancel_task",
        "Cancel a running A2A task",
        taskIdArg,
        output = taskOutput
    )
) { req ->
    clientFor(req.connectRequest ?: Request(GET, ""))
        .tasks().cancel(TaskId.of(taskIdArg(req)))
        .map { Ok().with(taskOutput of it) }
        .recover { Error(listOf(Text(it.toString()))) }
}
