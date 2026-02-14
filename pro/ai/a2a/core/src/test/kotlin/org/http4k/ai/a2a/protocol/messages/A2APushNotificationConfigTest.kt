package org.http4k.ai.a2a.protocol.messages

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.a2a.model.PushNotificationConfig
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskPushNotificationConfig
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Uri
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class A2APushNotificationConfigTest {

    private val taskId = TaskId.of("task-123")
    private val configId = PushNotificationConfigId.of("config-456")
    private val pushConfig = PushNotificationConfig(
        url = Uri.of("https://example.com/webhook"),
        token = "secret-token"
    )

    @Test
    fun `Set method is tasks_pushNotificationConfig_set`() {
        assertThat(A2APushNotificationConfig.Set.Method.value, equalTo("tasks/pushNotificationConfig/set"))
    }

    @Test
    fun `Set Request roundtrips correctly`(approver: Approver) {
        val request = A2APushNotificationConfig.Set.Request(
            taskId = taskId,
            pushNotificationConfig = pushConfig
        )
        val json = A2AJson.asFormatString(request)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2APushNotificationConfig.Set.Request>(json), equalTo(request))
    }

    @Test
    fun `Set Response roundtrips correctly`(approver: Approver) {
        val response = A2APushNotificationConfig.Set.Response(
            id = configId,
            taskId = taskId,
            pushNotificationConfig = pushConfig
        )
        val json = A2AJson.asFormatString(response)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2APushNotificationConfig.Set.Response>(json), equalTo(response))
    }

    @Test
    fun `Get method is tasks_pushNotificationConfig_get`() {
        assertThat(A2APushNotificationConfig.Get.Method.value, equalTo("tasks/pushNotificationConfig/get"))
    }

    @Test
    fun `Get Request roundtrips correctly`(approver: Approver) {
        val request = A2APushNotificationConfig.Get.Request(id = configId)
        val json = A2AJson.asFormatString(request)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2APushNotificationConfig.Get.Request>(json), equalTo(request))
    }

    @Test
    fun `Get Response roundtrips correctly`(approver: Approver) {
        val response = A2APushNotificationConfig.Get.Response(
            id = configId,
            taskId = taskId,
            pushNotificationConfig = pushConfig
        )
        val json = A2AJson.asFormatString(response)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2APushNotificationConfig.Get.Response>(json), equalTo(response))
    }

    @Test
    fun `List method is tasks_pushNotificationConfig_list`() {
        assertThat(A2APushNotificationConfig.List.Method.value, equalTo("tasks/pushNotificationConfig/list"))
    }

    @Test
    fun `List Request roundtrips correctly`(approver: Approver) {
        val request = A2APushNotificationConfig.List.Request(taskId = taskId)
        val json = A2AJson.asFormatString(request)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2APushNotificationConfig.List.Request>(json), equalTo(request))
    }

    @Test
    fun `List Response roundtrips correctly`(approver: Approver) {
        val response = A2APushNotificationConfig.List.Response(
            configs = listOf(
                TaskPushNotificationConfig(
                    id = configId,
                    taskId = taskId,
                    pushNotificationConfig = pushConfig
                )
            )
        )
        val json = A2AJson.asFormatString(response)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2APushNotificationConfig.List.Response>(json), equalTo(response))
    }

    @Test
    fun `Delete method is tasks_pushNotificationConfig_delete`() {
        assertThat(A2APushNotificationConfig.Delete.Method.value, equalTo("tasks/pushNotificationConfig/delete"))
    }

    @Test
    fun `Delete Request roundtrips correctly`(approver: Approver) {
        val request = A2APushNotificationConfig.Delete.Request(id = configId)
        val json = A2AJson.asFormatString(request)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2APushNotificationConfig.Delete.Request>(json), equalTo(request))
    }

    @Test
    fun `Delete Response roundtrips correctly`(approver: Approver) {
        val response = A2APushNotificationConfig.Delete.Response(id = configId)
        val json = A2AJson.asFormatString(response)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2APushNotificationConfig.Delete.Response>(json), equalTo(response))
    }
}
