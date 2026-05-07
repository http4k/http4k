/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.a2a.capabilities

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import org.http4k.ai.a2a.client.A2AClient
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.mcp.ToolResponse.Error
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.lens.with

fun ListTasks(client: A2AClient) = ToolCapability(
    Tool(
        "list_tasks",
        "List A2A tasks, optionally filtered by context or status",
        contextIdArg, statusFilterArg, pageSizeArg,
        output = taskPageOutput
    )
) {
    client.tasks().list(
        contextId = contextIdArg(it)?.let(ContextId::of),
        status = statusFilterArg(it)?.let(TaskState::valueOf),
        pageSize = pageSizeArg(it)
    )
        .map { Ok().with(taskPageOutput of it) }
        .recover { Error(listOf(Text(it.toString()))) }
}
