package org.http4k.ai.a2a.server.storage

import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.server.notification.PushNotificationSender
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool

fun TaskStorage.withPushNotifications(
    configStorage: PushNotificationConfigStorage,
    sender: PushNotificationSender,
    executor: Executor = ForkJoinPool.commonPool()
): TaskStorage = NotifyingTaskStorage(this, configStorage, sender, executor)

private class NotifyingTaskStorage(
    private val delegate: TaskStorage,
    private val configStorage: PushNotificationConfigStorage,
    private val sender: PushNotificationSender,
    private val executor: Executor
) : TaskStorage {

    override fun store(task: Task) {
        delegate.store(task)
        configStorage.list(task.id).forEach { config ->
            executor.execute { sender(task, config) }
        }
    }

    override fun get(taskId: TaskId): Task? = delegate.get(taskId)

    override fun delete(taskId: TaskId) = delegate.delete(taskId)

    override fun list(): List<Task> = delegate.list()

    override fun listByContext(contextId: ContextId): List<Task> = delegate.listByContext(contextId)
}
