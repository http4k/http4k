package org.http4k.ai.a2a.server.capability

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.protocol.messages.A2APushNotificationConfig
import org.http4k.ai.a2a.model.PushNotificationConfig
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

class ServerPushNotificationConfigsTest {

    private val configs = ServerPushNotificationConfigs()

    private fun aPushConfig(url: String = "https://example.com/webhook") =
        PushNotificationConfig(url = Uri.of(url))

    @Test
    fun `set creates new config and returns it with generated id`() {
        val taskId = TaskId.of("task-1")
        val pushConfig = aPushConfig()

        val result = configs.set(A2APushNotificationConfig.Set.Request(taskId, pushConfig))

        assertThat(result.taskId, equalTo(taskId))
        assertThat(result.pushNotificationConfig, equalTo(pushConfig))
        assertThat(result.id.value.isNotEmpty(), equalTo(true))
    }

    @Test
    fun `get returns existing config`() {
        val taskId = TaskId.of("task-1")
        val pushConfig = aPushConfig()
        val created = configs.set(A2APushNotificationConfig.Set.Request(taskId, pushConfig))

        val result = configs.get(A2APushNotificationConfig.Get.Request(created.id))

        assertThat(result, present())
        assertThat(result!!.id, equalTo(created.id))
        assertThat(result.taskId, equalTo(created.taskId))
        assertThat(result.pushNotificationConfig, equalTo(created.pushNotificationConfig))
    }

    @Test
    fun `get returns null for non-existent config`() {
        val result = configs.get(A2APushNotificationConfig.Get.Request(PushNotificationConfigId.of("non-existent")))

        assertThat(result, absent())
    }

    @Test
    fun `list returns empty list when no configs for task`() {
        val result = configs.list(A2APushNotificationConfig.List.Request(TaskId.of("task-1")))

        assertThat(result.configs, equalTo(emptyList()))
    }

    @Test
    fun `list returns configs for specific task`() {
        val taskId = TaskId.of("task-1")
        val config1 = configs.set(A2APushNotificationConfig.Set.Request(taskId, aPushConfig("https://example1.com")))
        val config2 = configs.set(A2APushNotificationConfig.Set.Request(taskId, aPushConfig("https://example2.com")))
        configs.set(A2APushNotificationConfig.Set.Request(TaskId.of("task-2"), aPushConfig("https://other.com")))

        val result = configs.list(A2APushNotificationConfig.List.Request(taskId))

        assertThat(result.configs.size, equalTo(2))
        assertThat(result.configs.map { it.id }.toSet(), equalTo(setOf(config1.id, config2.id)))
    }

    @Test
    fun `delete removes config and returns response with id`() {
        val taskId = TaskId.of("task-1")
        val created = configs.set(A2APushNotificationConfig.Set.Request(taskId, aPushConfig()))

        val result = configs.delete(A2APushNotificationConfig.Delete.Request(created.id))

        assertThat(result, present())
        assertThat(result!!.id, equalTo(created.id))
        assertThat(configs.get(A2APushNotificationConfig.Get.Request(created.id)), absent())
    }

    @Test
    fun `delete returns null for non-existent config`() {
        val result =
            configs.delete(A2APushNotificationConfig.Delete.Request(PushNotificationConfigId.of("non-existent")))

        assertThat(result, absent())
    }
}
