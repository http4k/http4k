package org.http4k.ai.a2a.server.storage

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.model.TaskStatus
import org.http4k.ai.a2a.model.PushNotificationConfig
import org.http4k.ai.a2a.model.TaskPushNotificationConfig
import org.http4k.ai.a2a.server.notification.PushNotificationSender
import org.http4k.core.Uri
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class NotifyingTaskStorageTest {

    private fun aTask(
        id: String = "task-1",
        contextId: String = "ctx-1",
        state: TaskState = TaskState.working
    ) = Task(
        id = TaskId.of(id),
        contextId = ContextId.of(contextId),
        status = TaskStatus(state = state)
    )

    private fun aConfig(
        id: String = "config-1",
        taskId: String = "task-1",
        url: String = "https://example.com/webhook"
    ) = TaskPushNotificationConfig(
        id = PushNotificationConfigId.of(id),
        taskId = TaskId.of(taskId),
        pushNotificationConfig = PushNotificationConfig(url = Uri.of(url))
    )

    @Test
    fun `store delegates to underlying storage`() {
        val taskStorage = TaskStorage.InMemory()
        val configStorage = PushNotificationConfigStorage.InMemory()
        val notifyingStorage = taskStorage.withPushNotifications(configStorage, PushNotificationSender.NoOp)

        val task = aTask()
        notifyingStorage.store(task)

        assertThat(taskStorage.get(task.id), equalTo(task))
    }

    @Test
    fun `get delegates to underlying storage`() {
        val taskStorage = TaskStorage.InMemory()
        val configStorage = PushNotificationConfigStorage.InMemory()
        val notifyingStorage = taskStorage.withPushNotifications(configStorage, PushNotificationSender.NoOp)

        val task = aTask()
        taskStorage.store(task)

        assertThat(notifyingStorage.get(task.id), equalTo(task))
    }

    @Test
    fun `delete delegates to underlying storage`() {
        val taskStorage = TaskStorage.InMemory()
        val configStorage = PushNotificationConfigStorage.InMemory()
        val notifyingStorage = taskStorage.withPushNotifications(configStorage, PushNotificationSender.NoOp)

        val task = aTask()
        taskStorage.store(task)
        notifyingStorage.delete(task.id)

        assertThat(taskStorage.get(task.id), equalTo(null))
    }

    @Test
    fun `list delegates to underlying storage`() {
        val taskStorage = TaskStorage.InMemory()
        val configStorage = PushNotificationConfigStorage.InMemory()
        val notifyingStorage = taskStorage.withPushNotifications(configStorage, PushNotificationSender.NoOp)

        taskStorage.store(aTask("task-1"))
        taskStorage.store(aTask("task-2"))

        assertThat(notifyingStorage.list().size, equalTo(2))
    }

    @Test
    fun `listByContext delegates to underlying storage`() {
        val taskStorage = TaskStorage.InMemory()
        val configStorage = PushNotificationConfigStorage.InMemory()
        val notifyingStorage = taskStorage.withPushNotifications(configStorage, PushNotificationSender.NoOp)

        taskStorage.store(aTask("task-1", "ctx-1"))
        taskStorage.store(aTask("task-2", "ctx-2"))

        assertThat(notifyingStorage.listByContext(ContextId.of("ctx-1")).size, equalTo(1))
    }

    @Test
    fun `store sends notification to registered config`() {
        val taskStorage = TaskStorage.InMemory()
        val configStorage = PushNotificationConfigStorage.InMemory()
        val sentNotifications = mutableListOf<Pair<Task, TaskPushNotificationConfig>>()
        val sender: PushNotificationSender = PushNotificationSender { task, config ->
            sentNotifications.add(task to config)
        }
        val notifyingStorage = taskStorage.withPushNotifications(
            configStorage, sender,
            Executors.newSingleThreadExecutor()
        )

        val config = aConfig("config-1", "task-1")
        configStorage.store(config)

        val task = aTask("task-1")
        notifyingStorage.store(task)

        Thread.sleep(100)

        assertThat(sentNotifications.size, equalTo(1))
        assertThat(sentNotifications[0].first, equalTo(task))
        assertThat(sentNotifications[0].second, equalTo(config))
    }

    @Test
    fun `store sends notification to all registered configs for task`() {
        val taskStorage = TaskStorage.InMemory()
        val configStorage = PushNotificationConfigStorage.InMemory()
        val sentNotifications = mutableListOf<Pair<Task, TaskPushNotificationConfig>>()
        val latch = CountDownLatch(2)
        val sender: PushNotificationSender = PushNotificationSender { task, config ->
            sentNotifications.add(task to config)
            latch.countDown()
        }
        val notifyingStorage = taskStorage.withPushNotifications(
            configStorage, sender,
            Executors.newFixedThreadPool(2)
        )

        val config1 = aConfig("config-1", "task-1", "https://example.com/webhook1")
        val config2 = aConfig("config-2", "task-1", "https://example.com/webhook2")
        configStorage.store(config1)
        configStorage.store(config2)

        val task = aTask("task-1")
        notifyingStorage.store(task)

        latch.await(1, TimeUnit.SECONDS)

        assertThat(sentNotifications.size, equalTo(2))
    }

    @Test
    fun `store sends no notification when no configs registered for task`() {
        val taskStorage = TaskStorage.InMemory()
        val configStorage = PushNotificationConfigStorage.InMemory()
        val sentNotifications = mutableListOf<Pair<Task, TaskPushNotificationConfig>>()
        val sender: PushNotificationSender = PushNotificationSender { task, config ->
            sentNotifications.add(task to config)
        }
        val notifyingStorage = taskStorage.withPushNotifications(configStorage, sender)

        val task = aTask("task-1")
        notifyingStorage.store(task)

        Thread.sleep(100)

        assertThat(sentNotifications.size, equalTo(0))
    }

    @Test
    fun `notifications are async and do not block store`() {
        val taskStorage = TaskStorage.InMemory()
        val configStorage = PushNotificationConfigStorage.InMemory()
        val latch = CountDownLatch(1)
        val sender: PushNotificationSender = PushNotificationSender { _, _ ->
            latch.await(5, TimeUnit.SECONDS)
        }
        val notifyingStorage = taskStorage.withPushNotifications(
            configStorage, sender,
            Executors.newSingleThreadExecutor()
        )

        val config = aConfig("config-1", "task-1")
        configStorage.store(config)

        val task = aTask("task-1")
        val startTime = System.currentTimeMillis()
        notifyingStorage.store(task)
        val endTime = System.currentTimeMillis()

        assertThat(endTime - startTime < 100, equalTo(true))

        latch.countDown()
    }
}
