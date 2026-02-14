package org.http4k.ai.a2a.server.capability

import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.model.TaskStatus
import org.http4k.ai.a2a.protocol.messages.A2ATask
import org.http4k.ai.a2a.server.protocol.Tasks
import org.http4k.ai.a2a.server.storage.TaskStorage

class ServerTasks(private val storage: TaskStorage = TaskStorage.InMemory()) : Tasks {

    override fun get(request: A2ATask.Get.Request): A2ATask.Get.Response? {
        val task = storage.get(request.id) ?: return null
        return A2ATask.Get.Response(task)
    }

    override fun store(task: Task) {
        storage.store(task)
    }

    override fun cancel(request: A2ATask.Cancel.Request): A2ATask.Cancel.Response? {
        val task = storage.get(request.id) ?: return null
        val cancelledTask = task.copy(status = TaskStatus(state = TaskState.canceled))
        storage.store(cancelledTask)
        return A2ATask.Cancel.Response(cancelledTask)
    }

    override fun list(request: A2ATask.List.Request): A2ATask.List.Response {
        val allTasks = storage.list()
            .filter { request.contextId == null || it.contextId == request.contextId }
            .filter { request.status == null || it.status.state == request.status }
            .sortedBy { it.id.value }

        val pageSize = request.pageSize
        val totalSize = allTasks.size
        val (pageTasks, nextToken) = if (pageSize != null) {
            val startIndex = request.pageToken?.toIntOrNull() ?: 0
            val endIndex = minOf(startIndex + pageSize, totalSize)
            allTasks.subList(startIndex, endIndex) to if (endIndex < totalSize) endIndex.toString() else ""
        } else {
            allTasks to ""
        }

        return A2ATask.List.Response(pageTasks, nextToken, pageSize, totalSize)
    }
}
