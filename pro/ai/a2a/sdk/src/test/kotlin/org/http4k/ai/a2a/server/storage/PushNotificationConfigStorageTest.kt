package org.http4k.ai.a2a.server.storage

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.PushNotificationConfig
import org.http4k.ai.a2a.model.TaskPushNotificationConfig
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

class PushNotificationConfigStorageTest {

    private val storage = PushNotificationConfigStorage.InMemory()

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
    fun `store and retrieve config`() {
        val config = aConfig()

        storage.store(config)
        val retrieved = storage.get(config.id)

        assertThat(retrieved, equalTo(config))
    }

    @Test
    fun `get returns null for non-existent config`() {
        val result = storage.get(PushNotificationConfigId.of("non-existent"))

        assertThat(result, absent())
    }

    @Test
    fun `store updates existing config`() {
        val config = aConfig()
        val updatedConfig = config.copy(
            pushNotificationConfig = PushNotificationConfig(url = Uri.of("https://updated.com/webhook"))
        )

        storage.store(config)
        storage.store(updatedConfig)
        val retrieved = storage.get(config.id)

        assertThat(retrieved, equalTo(updatedConfig))
    }

    @Test
    fun `delete removes config`() {
        val config = aConfig()

        storage.store(config)
        storage.delete(config.id)
        val retrieved = storage.get(config.id)

        assertThat(retrieved, absent())
    }

    @Test
    fun `delete non-existent config does nothing`() {
        storage.delete(PushNotificationConfigId.of("non-existent"))
    }

    @Test
    fun `listByTask returns empty list when no configs for task`() {
        val result = storage.list(TaskId.of("task-1"))

        assertThat(result, equalTo(emptyList()))
    }

    @Test
    fun list() {
        val config1 = aConfig("config-1", "task-1")
        val config2 = aConfig("config-2", "task-1")
        val config3 = aConfig("config-3", "task-2")

        storage.store(config1)
        storage.store(config2)
        storage.store(config3)

        val result = storage.list(TaskId.of("task-1"))

        assertThat(result.size, equalTo(2))
        assertThat(result.map { it.id }.toSet(), equalTo(setOf(config1.id, config2.id)))
    }
}
