/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import org.http4k.ai.a2a.A2ARequest
import org.http4k.ai.a2a.A2AResponse
import org.http4k.ai.a2a.MessageHandler
import org.http4k.ai.a2a.MessageResponse
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.Part
import org.http4k.ai.a2a.model.PushNotificationConfig
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.model.TaskState.*
import org.http4k.ai.a2a.model.TaskStatus
import org.http4k.ai.a2a.protocol.messages.A2AJsonRpcErrorResponse
import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.ai.a2a.protocol.messages.A2APushNotificationConfig
import org.http4k.ai.a2a.protocol.messages.A2ATask
import org.http4k.ai.a2a.server.storage.PushNotificationConfigStorage
import org.http4k.ai.a2a.server.storage.TaskStorage
import org.http4k.ai.model.Role
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

class A2AHandlerTest {

    private val tasks = TaskStorage.InMemory()
    private val pushNotifications = PushNotificationConfigStorage.InMemory()

    private fun aTask(id: String = "task-1", contextId: String = "ctx-1") = Task(
        id = TaskId.of(id),
        contextId = ContextId.of(contextId),
        status = TaskStatus(state = completed),
        history = listOf(Message(role = Role.User, parts = listOf(Part.Text("hello"))))
    )

    private fun aMessage() = Message(role = Role.User, parts = listOf(Part.Text("hello")))

    private fun handlerReturningTask(vararg taskStates: TaskState): MessageHandler = { request ->
        val taskSeq = taskStates.map { state ->
            Task(
                id = TaskId.of("task-1"),
                contextId = ContextId.of("ctx-1"),
                status = TaskStatus(state = state),
                history = listOf(request.message)
            )
        }.asSequence()
        MessageResponse.Task(taskSeq)
    }

    private fun handlerReturningMessage(): MessageHandler = { request ->
        MessageResponse.Message(Message(role = Role.Assistant, parts = listOf(Part.Text("response"))))
    }

    private fun send(request: org.http4k.ai.a2a.protocol.messages.A2AJsonRpcRequest): A2AResponse {
        val handler = RoutingA2AHandler(handlerReturningTask(completed), tasks, pushNotifications)
        return handler(A2ARequest(request, Request(POST, "/")))
    }

    @Test
    fun `message send returns task response`() {
        val handler = RoutingA2AHandler(handlerReturningTask(completed), tasks, pushNotifications)
        val result = handler(
            A2ARequest(
                A2AMessage.Send.Request(A2AMessage.Send.Request.Params(aMessage()), "1"),
                Request(POST, "/")
            )
        )

        assertThat(result, isA<A2AResponse.Single>())
        val response = (result as A2AResponse.Single).message
        assertThat(response, isA<A2AMessage.Send.Response.Task>())
        assertThat((response as A2AMessage.Send.Response.Task).result.status.state, equalTo(completed))
    }

    @Test
    fun `message send returns message response`() {
        val handler = RoutingA2AHandler(handlerReturningMessage(), tasks, pushNotifications)
        val result = handler(
            A2ARequest(
                A2AMessage.Send.Request(A2AMessage.Send.Request.Params(aMessage()), "1"),
                Request(POST, "/")
            )
        )

        assertThat(result, isA<A2AResponse.Single>())
        val response = (result as A2AResponse.Single).message
        assertThat(response, isA<A2AMessage.Send.Response.Message>())
    }

    @Test
    fun `message stream returns stream of task responses`() {
        val handler = RoutingA2AHandler(handlerReturningTask(working, completed), tasks, pushNotifications)
        val result = handler(
            A2ARequest(
                A2AMessage.Stream.Request(A2AMessage.Stream.Request.Params(aMessage()), "1"),
                Request(POST, "/")
            )
        )

        assertThat(result, isA<A2AResponse.Stream>())
        val responses = (result as A2AResponse.Stream).messages.toList()
        assertThat(responses.size, equalTo(2))
        assertThat((responses[0] as A2AMessage.Send.Response.Task).result.status.state, equalTo(working))
        assertThat((responses[1] as A2AMessage.Send.Response.Task).result.status.state, equalTo(completed))
    }

    @Test
    fun `tasks get returns stored task`() {
        val task = aTask()
        tasks.store(task)

        val result = send(A2ATask.Get.Request(A2ATask.Get.Request.Params(task.id), "1"))

        val response = (result as A2AResponse.Single).message as A2ATask.Get.Response
        assertThat(response.result.task.id, equalTo(task.id))
    }

    @Test
    fun `tasks get returns error for unknown task`() {
        val result = send(A2ATask.Get.Request(A2ATask.Get.Request.Params(TaskId.of("unknown")), "1"))
        assertThat((result as A2AResponse.Single).message, isA<A2AJsonRpcErrorResponse>())
    }

    @Test
    fun `tasks cancel cancels task`() {
        val task = aTask()
        tasks.store(task)

        val result = send(A2ATask.Cancel.Request(A2ATask.Cancel.Request.Params(task.id), "1"))

        val response = (result as A2AResponse.Single).message as A2ATask.Cancel.Response
        assertThat(response.result.task.status.state, equalTo(canceled))
    }

    @Test
    fun `tasks list returns stored tasks`() {
        tasks.store(aTask("t1"))
        tasks.store(aTask("t2"))

        val result = send(A2ATask.List.Request(A2ATask.List.Request.Params(), "1"))

        val response = (result as A2AResponse.Single).message as A2ATask.List.Response
        assertThat(response.result.tasks.size, equalTo(2))
        assertThat(response.result.totalSize, equalTo(2))
    }

    @Test
    fun `push notification config set and get`() {
        tasks.store(aTask())
        val handler = RoutingA2AHandler(handlerReturningTask(completed), tasks, pushNotifications)

        val setResult = handler(
            A2ARequest(
                A2APushNotificationConfig.Set.Request(
                    A2APushNotificationConfig.Set.Request.Params(
                        TaskId.of("task-1"),
                        PushNotificationConfig(url = Uri.of("https://example.com/webhook"))
                    ), "1"
                ),
                Request(POST, "/")
            )
        )

        val setResponse = (setResult as A2AResponse.Single).message as A2APushNotificationConfig.Set.Response
        assertThat(setResponse.result.taskId, equalTo(TaskId.of("task-1")))

        val getResult = handler(
            A2ARequest(
                A2APushNotificationConfig.Get.Request(
                    A2APushNotificationConfig.Get.Request.Params(setResponse.result.id), "2"
                ),
                Request(POST, "/")
            )
        )

        val getResponse = (getResult as A2AResponse.Single).message as A2APushNotificationConfig.Get.Response
        assertThat(getResponse.result.id, equalTo(setResponse.result.id))
    }

    @Test
    fun `push notification config list`() {
        val handler = RoutingA2AHandler(handlerReturningTask(completed), tasks, pushNotifications)
        val taskId = TaskId.of("task-1")

        handler(
            A2ARequest(
                A2APushNotificationConfig.Set.Request(
                    A2APushNotificationConfig.Set.Request.Params(taskId, PushNotificationConfig(url = Uri.of("https://a.com"))),
                    "1"
                ),
                Request(POST, "/")
            )
        )
        handler(
            A2ARequest(
                A2APushNotificationConfig.Set.Request(
                    A2APushNotificationConfig.Set.Request.Params(taskId, PushNotificationConfig(url = Uri.of("https://b.com"))),
                    "2"
                ),
                Request(POST, "/")
            )
        )

        val listResult = handler(
            A2ARequest(
                A2APushNotificationConfig.List.Request(
                    A2APushNotificationConfig.List.Request.Params(taskId), "3"
                ),
                Request(POST, "/")
            )
        )

        val listResponse = (listResult as A2AResponse.Single).message as A2APushNotificationConfig.List.Response
        assertThat(listResponse.result.configs.size, equalTo(2))
    }

    @Test
    fun `push notification config delete`() {
        val handler = RoutingA2AHandler(handlerReturningTask(completed), tasks, pushNotifications)
        val taskId = TaskId.of("task-1")

        val setResult = handler(
            A2ARequest(
                A2APushNotificationConfig.Set.Request(
                    A2APushNotificationConfig.Set.Request.Params(taskId, PushNotificationConfig(url = Uri.of("https://a.com"))),
                    "1"
                ),
                Request(POST, "/")
            )
        )
        val configId = ((setResult as A2AResponse.Single).message as A2APushNotificationConfig.Set.Response).result.id

        val deleteResult = handler(
            A2ARequest(
                A2APushNotificationConfig.Delete.Request(
                    A2APushNotificationConfig.Delete.Request.Params(configId), "2"
                ),
                Request(POST, "/")
            )
        )

        val deleteResponse = (deleteResult as A2AResponse.Single).message as A2APushNotificationConfig.Delete.Response
        assertThat(deleteResponse.result.id, equalTo(configId))

        val listResult = handler(
            A2ARequest(
                A2APushNotificationConfig.List.Request(
                    A2APushNotificationConfig.List.Request.Params(taskId), "3"
                ),
                Request(POST, "/")
            )
        )
        val listResponse = (listResult as A2AResponse.Single).message as A2APushNotificationConfig.List.Response
        assertThat(listResponse.result.configs.size, equalTo(0))
    }
}
