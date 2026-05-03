/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.a2a.model.AuthScheme.Companion.BEARER
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Uri
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class PushNotificationTest {

    @Test
    fun `TaskPushNotificationConfig roundtrips correctly`(approver: Approver) {
        val config = TaskPushNotificationConfig(
            id = PushNotificationConfigId.of("config-1"),
            taskId = TaskId.of("task-1"),
            url = Uri.of("https://example.com/webhook"),
            token = "secret-token",
            authentication = AuthenticationInfo(scheme = BEARER)
        )
        val json = A2AJson.asFormatString(config)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<TaskPushNotificationConfig>(json), equalTo(config))
    }

    @Test
    fun `TaskPushNotificationConfig without optional fields roundtrips correctly`(approver: Approver) {
        val config = TaskPushNotificationConfig(
            id = PushNotificationConfigId.of("config-1"),
            taskId = TaskId.of("task-1"),
            url = Uri.of("https://example.com/webhook")
        )
        val json = A2AJson.asFormatString(config)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<TaskPushNotificationConfig>(json), equalTo(config))
    }

    @Test
    fun `PushNotificationConfigId is a value type`() {
        val id = PushNotificationConfigId.of("config-123")
        assertThat(id.value, equalTo("config-123"))
    }
}
