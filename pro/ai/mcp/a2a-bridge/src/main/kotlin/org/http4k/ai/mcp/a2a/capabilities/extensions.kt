/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.a2a.capabilities

import org.http4k.ai.a2a.model.A2ARole.ROLE_USER
import org.http4k.ai.a2a.model.Artifact
import org.http4k.ai.a2a.model.ArtifactId
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.MessageId
import org.http4k.ai.a2a.model.PageToken
import org.http4k.ai.a2a.model.Part.Text
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskPage
import org.http4k.ai.a2a.model.TaskState.TASK_STATE_WORKING
import org.http4k.ai.a2a.model.TaskStatus
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.int
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.a2a.BridgeJson.auto
import java.time.Instant.EPOCH

val messageArg = Tool.Arg.string().required("message", "message to send to the agent")
val contextIdArg = Tool.Arg.string().optional("contextId", "context ID for multi-turn conversations")
val taskIdArg = Tool.Arg.string().required("taskId", "ID of the task")
val historyLengthArg = Tool.Arg.int().optional("historyLength", "number of history messages to include")
val statusFilterArg = Tool.Arg.string().optional("status", "filter by task state (e.g. TASK_STATE_WORKING)")
val pageSizeArg = Tool.Arg.int().optional("pageSize", "number of tasks to return")

data class SendMessageResult(
    val message: Message? = null,
    val task: Task? = null
)

private val exampleMessage = Message(
    messageId = MessageId.of("example-message-id"),
    role = ROLE_USER,
    parts = listOf(Text("example response text")),
    contextId = ContextId.of("example-context-id")
)

val taskOutput = Tool.Output.auto(
    Task(
        id = TaskId.of("example-task-id"),
        status = TaskStatus(state = TASK_STATE_WORKING, message = exampleMessage, timestamp = EPOCH),
        contextId = ContextId.of("example-context-id"),
        artifacts = listOf(
            Artifact(
                artifactId = ArtifactId.of("example-artifact-id"),
                parts = listOf(Text("example artifact content"))
            )
        ),
        history = listOf(exampleMessage)
    )
).toLens()

val taskPageOutput = Tool.Output.auto(
    TaskPage(
        tasks = listOf(
            Task(
                id = TaskId.of("example-task-id"),
                status = TaskStatus(state = TASK_STATE_WORKING, message = exampleMessage, timestamp = EPOCH),
                contextId = ContextId.of("example-context-id"),
                artifacts = listOf(
                    Artifact(
                        artifactId = ArtifactId.of("example-artifact-id"),
                        parts = listOf(Text("example artifact content"))
                    )
                ),
                history = listOf(exampleMessage)
            )
        ),
        nextPageToken = PageToken.END,
        pageSize = 10,
        totalSize = 1
    )
).toLens()

val sendMessageOutput = Tool.Output.auto(
    SendMessageResult(
        message = exampleMessage, task = Task(
            id = TaskId.of("example-task-id"),
            status = TaskStatus(state = TASK_STATE_WORKING, message = exampleMessage, timestamp = EPOCH),
            contextId = ContextId.of("example-context-id"),
            artifacts = listOf(
                Artifact(
                    artifactId = ArtifactId.of("example-artifact-id"),
                    parts = listOf(Text("example artifact content"))
                )
            ),
            history = listOf(exampleMessage)
        )
    )
).toLens()
