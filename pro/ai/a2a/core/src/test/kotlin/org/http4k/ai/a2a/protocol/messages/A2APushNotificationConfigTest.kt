/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.protocol.messages

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskPushNotificationConfig
import org.http4k.ai.a2a.protocol.A2ARpcMethod
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
    private val webhookUrl = Uri.of("https://example.com/webhook")

    @Test
    fun `Set method is CreateTaskPushNotificationConfig`() {
        assertThat(
            A2APushNotificationConfig.Set.Request(
                params = A2APushNotificationConfig.Set.Request.Params(
                    taskId = taskId,
                    url = webhookUrl
                ),
                id = "1"
            ).method, equalTo(A2ARpcMethod.of("CreateTaskPushNotificationConfig"))
        )
    }

    @Test
    fun `Set Request roundtrips correctly`(approver: Approver) {
        val request = A2APushNotificationConfig.Set.Request(
            params = A2APushNotificationConfig.Set.Request.Params(
                taskId = taskId,
                url = webhookUrl,
                token = "secret-token"
            ),
            id = "1"
        )
        val json = A2AJson.asFormatString(request)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2APushNotificationConfig.Set.Request>(json), equalTo(request))
    }

    @Test
    fun `Set Response roundtrips correctly`(approver: Approver) {
        val response = A2APushNotificationConfig.Set.Response(
            result = TaskPushNotificationConfig(
                id = configId,
                taskId = taskId,
                url = webhookUrl,
                token = "secret-token"
            ),
            id = "1"
        )
        val json = A2AJson.asFormatString(response)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2APushNotificationConfig.Set.Response>(json), equalTo(response))
    }

    @Test
    fun `Get method is GetTaskPushNotificationConfig`() {
        assertThat(
            A2APushNotificationConfig.Get.Request(
                params = A2APushNotificationConfig.Get.Request.Params(taskId = taskId, id = configId),
                id = "1"
            ).method, equalTo(A2ARpcMethod.of("GetTaskPushNotificationConfig"))
        )
    }

    @Test
    fun `Get Request roundtrips correctly`(approver: Approver) {
        val request = A2APushNotificationConfig.Get.Request(
            params = A2APushNotificationConfig.Get.Request.Params(taskId = taskId, id = configId),
            id = "1"
        )
        val json = A2AJson.asFormatString(request)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2APushNotificationConfig.Get.Request>(json), equalTo(request))
    }

    @Test
    fun `Get Response roundtrips correctly`(approver: Approver) {
        val response = A2APushNotificationConfig.Get.Response(
            result = TaskPushNotificationConfig(
                id = configId,
                taskId = taskId,
                url = webhookUrl
            ),
            id = "1"
        )
        val json = A2AJson.asFormatString(response)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2APushNotificationConfig.Get.Response>(json), equalTo(response))
    }

    @Test
    fun `List method is ListTaskPushNotificationConfigs`() {
        assertThat(
            A2APushNotificationConfig.List.Request(
                params = A2APushNotificationConfig.List.Request.Params(taskId = taskId),
                id = "1"
            ).method, equalTo(A2ARpcMethod.of("ListTaskPushNotificationConfigs"))
        )
    }

    @Test
    fun `List Request roundtrips correctly`(approver: Approver) {
        val request = A2APushNotificationConfig.List.Request(
            params = A2APushNotificationConfig.List.Request.Params(taskId = taskId),
            id = "1"
        )
        val json = A2AJson.asFormatString(request)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2APushNotificationConfig.List.Request>(json), equalTo(request))
    }

    @Test
    fun `List Response roundtrips correctly`(approver: Approver) {
        val response = A2APushNotificationConfig.List.Response(
            result = A2APushNotificationConfig.List.Response.Result(
                configs = listOf(
                    TaskPushNotificationConfig(
                        id = configId,
                        taskId = taskId,
                        url = webhookUrl
                    )
                )
            ),
            id = "1"
        )
        val json = A2AJson.asFormatString(response)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2APushNotificationConfig.List.Response>(json), equalTo(response))
    }

    @Test
    fun `Delete method is DeleteTaskPushNotificationConfig`() {
        assertThat(
            A2APushNotificationConfig.Delete.Request(
                params = A2APushNotificationConfig.Delete.Request.Params(taskId = taskId, id = configId),
                id = "1"
            ).method, equalTo(A2ARpcMethod.of("DeleteTaskPushNotificationConfig"))
        )
    }

    @Test
    fun `Delete Request roundtrips correctly`(approver: Approver) {
        val request = A2APushNotificationConfig.Delete.Request(
            params = A2APushNotificationConfig.Delete.Request.Params(taskId = taskId, id = configId),
            id = "1"
        )
        val json = A2AJson.asFormatString(request)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2APushNotificationConfig.Delete.Request>(json), equalTo(request))
    }

    @Test
    fun `Delete Response roundtrips correctly`(approver: Approver) {
        val response = A2APushNotificationConfig.Delete.Response(
            result = A2APushNotificationConfig.Delete.Response.Result(id = configId),
            id = "1"
        )
        val json = A2AJson.asFormatString(response)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2APushNotificationConfig.Delete.Response>(json), equalTo(response))
    }
}
