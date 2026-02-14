package org.http4k.ai.a2a.server.protocol

import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.protocol.messages.A2ATask

/**
 * Handles task lifecycle in the A2A protocol.
 * Tasks are the core concept - messages create or update tasks.
 */
interface Tasks {
    fun get(request: A2ATask.Get.Request): A2ATask.Get.Response?
    fun store(task: Task)
    fun cancel(request: A2ATask.Cancel.Request): A2ATask.Cancel.Response?
    fun list(request: A2ATask.List.Request): A2ATask.List.Response
}
