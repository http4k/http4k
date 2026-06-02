/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server.notification

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskPushNotificationConfig
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.model.TaskStatus
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

class PushNotificationUrlPolicyTest {

    private val Default = PushNotificationUrlPolicy.Default

    @Test
    fun `rejects loopback address`() {
        assertThat(Default(Uri.of("http://127.0.0.1/cb")), equalTo(false))
        assertThat(Default(Uri.of("http://localhost/cb")), equalTo(false))
    }

    @Test
    fun `rejects link-local cloud-metadata address`() {
        assertThat(Default(Uri.of("http://169.254.169.254/latest/meta-data/")), equalTo(false))
    }

    @Test
    fun `rejects RFC1918 private ranges`() {
        assertThat(Default(Uri.of("http://10.0.0.1/cb")), equalTo(false))
        assertThat(Default(Uri.of("http://192.168.1.1/cb")), equalTo(false))
        assertThat(Default(Uri.of("http://172.16.0.1/cb")), equalTo(false))
    }

    @Test
    fun `rejects wildcard address`() {
        assertThat(Default(Uri.of("http://0.0.0.0/cb")), equalTo(false))
    }

    @Test
    fun `rejects non-http schemes`() {
        assertThat(Default(Uri.of("file:///etc/passwd")), equalTo(false))
        assertThat(Default(Uri.of("gopher://example.com/")), equalTo(false))
        assertThat(Default(Uri.of("ftp://example.com/")), equalTo(false))
    }

    @Test
    fun `rejects empty host`() {
        assertThat(Default(Uri.of("http:///cb")), equalTo(false))
    }

    @Test
    fun `allows public IPv4`() {
        assertThat(Default(Uri.of("https://8.8.8.8/cb")), equalTo(true))
    }

    @Test
    fun `Http sender does not invoke handler when policy rejects URL`() {
        val invoked = AtomicBoolean(false)
        val http = { _: Request ->
            invoked.set(true)
            Response(OK)
        }

        val sender = PushNotificationSender.Http(http, PushNotificationUrlPolicy.Default)

        sender(
            Task(
                id = TaskId.of("t"),
                contextId = ContextId.of("c"),
                status = TaskStatus(state = TaskState.TASK_STATE_WORKING)
            ),
            TaskPushNotificationConfig(
                id = PushNotificationConfigId.of("p"),
                taskId = TaskId.of("t"),
                url = Uri.of("http://127.0.0.1/cb")
            )
        )

        assertThat(invoked.get(), equalTo(false))
    }

    @Test
    fun `Http sender consults a custom policy`() {
        val invoked = AtomicBoolean(false)
        val http = { _: Request ->
            invoked.set(true)
            Response(OK)
        }

        val sender = PushNotificationSender.Http(http) { uri -> uri.host == "approved.example" }

        sender(
            Task(
                id = TaskId.of("t"),
                contextId = ContextId.of("c"),
                status = TaskStatus(state = TaskState.TASK_STATE_WORKING)
            ),
            TaskPushNotificationConfig(
                id = PushNotificationConfigId.of("p"),
                taskId = TaskId.of("t"),
                url = Uri.of("http://denied.example/cb")
            )
        )

        assertThat(invoked.get(), equalTo(false))
    }
}
