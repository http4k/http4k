/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server.notification

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.a2a.model.AuthScheme
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.model.TaskStatus
import org.http4k.ai.a2a.model.AuthenticationInfo
import org.http4k.ai.a2a.model.PushNotificationConfig
import org.http4k.ai.a2a.model.TaskPushNotificationConfig
import org.http4k.ai.a2a.protocol.messages.toWire
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasMethod
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference

class PushNotificationSenderTest {

    private fun aTask(id: String = "task-1") = Task(
        id = TaskId.of(id),
        contextId = ContextId.of("ctx-1"),
        status = TaskStatus(state = TaskState.TASK_STATE_WORKING)
    )

    private fun aConfig(
        id: String = "config-1",
        taskId: String = "task-1",
        url: String = "https://example.com/webhook",
        token: String? = null,
        auth: AuthenticationInfo? = null
    ) = TaskPushNotificationConfig(
        id = PushNotificationConfigId.of(id),
        taskId = TaskId.of(taskId),
        pushNotificationConfig = PushNotificationConfig(
            url = Uri.of(url),
            token = token,
            authentication = auth
        )
    )

    @Test
    fun `sends POST request to webhook URL with task JSON payload`() {
        val capturedRequest = AtomicReference<Request>()
        val http = { req: Request ->
            capturedRequest.set(req)
            Response(OK)
        }

        val sender = PushNotificationSender.Http(http)
        val task = aTask()
        val config = aConfig()

        sender(task, config)

        assertThat(capturedRequest.get(), hasMethod(POST))
        assertThat(capturedRequest.get().uri, equalTo(Uri.of("https://example.com/webhook")))
        assertThat(capturedRequest.get(), hasBody(A2AJson.asFormatString(task.toWire())))
        assertThat(capturedRequest.get(), hasHeader("Content-Type", "application/json; charset=utf-8"))
    }

    @Test
    fun `adds Authorization Bearer header when bearer auth configured`() {
        val capturedRequest = AtomicReference<Request>()
        val http = { req: Request ->
            capturedRequest.set(req)
            Response(OK)
        }

        val sender = PushNotificationSender.Http(http)
        val task = aTask()
        val config = aConfig(
            token = "my-secret-token",
            auth = AuthenticationInfo(scheme = AuthScheme.BEARER)
        )

        sender(task, config)

        assertThat(capturedRequest.get(), hasHeader("Authorization", "Bearer my-secret-token"))
    }

    @Test
    fun `adds X-API-Key header when API key auth configured`() {
        val capturedRequest = AtomicReference<Request>()
        val http = { req: Request ->
            capturedRequest.set(req)
            Response(OK)
        }

        val sender = PushNotificationSender.Http(http)
        val task = aTask()
        val config = aConfig(
            token = "my-api-key",
            auth = AuthenticationInfo(scheme = AuthScheme.API_KEY)
        )

        sender(task, config)

        assertThat(capturedRequest.get(), hasHeader("X-API-Key", "my-api-key"))
    }

    @Test
    fun `catches HTTP errors without propagating`() {
        val http = { _: Request -> Response(INTERNAL_SERVER_ERROR) }

        val sender = PushNotificationSender.Http(http)
        val task = aTask()
        val config = aConfig()

        sender(task, config)
    }

    @Test
    fun `catches exceptions without propagating and calls error handler`() {
        val http = { _: Request -> throw RuntimeException("Connection failed") }

        val sender = PushNotificationSender.Http(http)
        val task = aTask()
        val config = aConfig()

        try {
            sender(task, config)
        } catch (e: RuntimeException) {
            assertThat(e.message, equalTo("Connection failed"))
        }
    }

    @Test
    fun `NoOp sender does nothing`() {
        val task = aTask()
        val config = aConfig()

        PushNotificationSender.NoOp(task, config)
    }
}
