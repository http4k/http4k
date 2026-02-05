package org.http4k.ai.mcp.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import com.natpryce.hamkrest.present
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.ElicitationRequest
import org.http4k.ai.mcp.ElicitationResponse
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.SamplingRequest
import org.http4k.ai.mcp.SamplingResponse
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Elicitation
import org.http4k.ai.mcp.model.ElicitationAction
import org.http4k.ai.mcp.model.ElicitationId
import org.http4k.ai.mcp.model.ElicitationModel
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Progress
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.model.Task
import org.http4k.ai.mcp.model.TaskId
import org.http4k.ai.mcp.model.TaskStatus
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.server.capability.ServerResources
import org.http4k.ai.mcp.server.capability.ServerTasks
import org.http4k.ai.mcp.server.capability.ServerTools
import org.http4k.ai.model.MaxTokens
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.Role.Companion.Assistant
import org.http4k.ai.model.StopReason
import org.http4k.ai.model.ToolName
import org.http4k.core.Uri
import org.http4k.format.auto
import org.http4k.lens.with
import org.http4k.routing.bind
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicReference

/**
 * Contract for MCP clients that support streaming capabilities (sampling, elicitation, progress).
 */
interface McpStreamingClientContract<T> : McpClientContract<T> {

    @Test
    fun `can do sampling`() {
        val model = ModelName.of("my model")

        val samplingResponses = listOf(
            SamplingResponse.Ok(model, Assistant, listOf(Content.Text("hello")), null),
            SamplingResponse.Ok(model, Assistant, listOf(Content.Text("world")), StopReason.of("foobar"))
        )

        val tools = ServerTools(
            Tool("sample", "description") bind {
                val received = it.client.sample(
                    SamplingRequest(listOf(), MaxTokens.of(1)),
                    Duration.ofSeconds(1)
                ).toList()
                assertThat(received, equalTo(samplingResponses.map { Success(it) }))
                Ok(listOf(Content.Text(received.size.toString())))
            }
        )

        withMcpServer(tools = tools) {
            sampling().onSampled { samplingResponses.asSequence() }

            assertThat(
                tools().call(ToolName.of("sample"), ToolRequest(meta = Meta("sample"))),
                equalTo(Success(Ok(Content.Text("2"))))
            )

            assertThat(
                tools().call(ToolName.of("sample"), ToolRequest(meta = Meta("sample"))),
                equalTo(Success(Ok(Content.Text("2"))))
            )
        }
    }

    class StreamingFooBar : ElicitationModel() {
        var foo by string("foo", "bar")
        var bar by optionalString("", "")
    }

    @Test
    fun `can do elicitation`() {
        val output = Elicitation.auto(StreamingFooBar()).toLens("name", "it's a name")

        val response = StreamingFooBar().apply { foo = "foo" }
        val elicitationId = ElicitationId.of("test-elicitation-123")

        val tools = ServerTools(
            Tool("elicit", "description") bind {
                val request = ElicitationRequest.Form("foobar", output, progressToken = it.meta.progressToken)
                val received = it.client.elicit(request, Duration.ofSeconds(1))

                assertThat(
                    received,
                    equalTo(
                        Success(
                            ElicitationResponse.Ok(ElicitationAction.valueOf(it.meta.progressToken!!.toString())).with(
                                output of response
                            )
                        )
                    )
                )

                val ok = received.valueOrNull()!! as ElicitationResponse.Ok
                assertThat(output(ok), equalTo(response))

                it.client.elicitationComplete(elicitationId)

                Ok(listOf(Content.Text(ok.action.name)))
            }
        )

        withMcpServer(tools = tools) {
            val receivedElicitationId = AtomicReference<ElicitationId>()
            elicitations().onComplete { receivedElicitationId.set(it) }

            elicitations().onElicitation {
                ElicitationResponse.Ok(ElicitationAction.valueOf(it.progressToken!!.toString()))
                    .with(output of response)
            }

            assertThat(
                tools().call(ToolName.of("elicit"), ToolRequest(meta = Meta("accept"))),
                equalTo(Success(Ok(Content.Text("accept"))))
            )

            assertThat(receivedElicitationId.get(), equalTo(elicitationId))

            assertThat(
                tools().call(ToolName.of("elicit"), ToolRequest(meta = Meta("decline"))),
                equalTo(Success(Ok(Content.Text("decline"))))
            )

            assertThat(
                tools().call(ToolName.of("elicit"), ToolRequest(meta = Meta("cancel"))),
                equalTo(Success(Ok(Content.Text("cancel"))))
            )
        }

    }

    @Test
    fun `can do elicitation with task response`() {
        val output = Elicitation.auto(StreamingFooBar()).toLens("name", "it's a name")
        val taskId = TaskId.of("elicitation-task-123")
        val now = Instant.now()
        val expectedTask = Task(taskId, TaskStatus.working, "Processing elicitation...", now, now)

        val tools = ServerTools(
            Tool("elicit-task", "description") bind {
                val request = ElicitationRequest.Form("foobar", output, progressToken = it.meta.progressToken)
                val received = it.client.elicit(request, Duration.ofSeconds(1))

                assertThat(received, present(isA<Success<ElicitationResponse.Task>>()))
                val taskResponse = received.valueOrNull() as ElicitationResponse.Task
                assertThat(taskResponse.task.taskId, equalTo(taskId))
                assertThat(taskResponse.task.status, equalTo(TaskStatus.working))

                Ok(listOf(Content.Text("done")))
            }
        )

        withMcpServer(tools = tools) {
            elicitations().onElicitation {
                ElicitationResponse.Task(expectedTask)
            }

            assertThat(
                tools().call(ToolName.of("elicit-task"), ToolRequest(meta = Meta("elicit-task"))),
                equalTo(Success(Ok(Content.Text("done"))))
            )
        }
    }

    @Test
    fun `can do progress`() {
        val tools = ServerTools(
            Tool("progress", "description") bind {
                it.client.progress(1, 2.0)
                Ok(listOf(Content.Text("")))
            }
        )

        withMcpServer(tools = tools) {
            val prog = AtomicReference<Progress>()
            progress().onProgress(fn = prog::set)

            assertThat(
                tools().call(ToolName.of("progress"), ToolRequest(meta = Meta("progress"))),
                equalTo(Success(Ok(Content.Text(""))))
            )

            assertThat(prog.get(), equalTo(Progress("progress", 1, 2.0)))
        }
    }

    @Test
    fun `task onUpdate callback receives task updates`() {
        val taskId = TaskId.of("callback-task")
        val now = Instant.now()
        val workingTask = Task(taskId, TaskStatus.working, "Processing...", now, now)

        val receivedTask = AtomicReference<Task>()
        val latch = CountDownLatch(1)

        val tools = ServerTools(
            Tool("start-task", "starts a task") bind {
                it.client.updateTask(workingTask)
                Ok(Content.Text("started"))
            }
        )

        withMcpServer(tools = tools) {
            tasks().onUpdate { t, _ ->
                receivedTask.set(t)
                latch.countDown()
            }

            tools().call(ToolName.of("start-task"), ToolRequest(meta = Meta("tasks")))

            assertThat(latch.await(5, SECONDS), equalTo(true))
            assertThat(receivedTask.get().taskId, equalTo(taskId))
        }
    }

    @Test
    fun `server Tasks onUpdate callback receives client task updates`() {
        val taskId = TaskId.of("server-callback-task")
        val now = Instant.now()
        val task = Task(taskId, TaskStatus.working, "Client processing...", now, now)

        val receivedTask = AtomicReference<Task>()
        val receivedMeta = AtomicReference<Meta>()
        val latch = CountDownLatch(1)

        val serverTasks = ServerTasks()
        serverTasks.onUpdate { t, m ->
            receivedTask.set(t)
            receivedMeta.set(m)
            latch.countDown()
        }

        withMcpServer(tasks = serverTasks) {
            tasks().update(task, Meta(progressToken = "server-token"))

            assertThat(latch.await(5, SECONDS), equalTo(true))
            assertThat(receivedTask.get().taskId, equalTo(taskId))
            assertThat(receivedTask.get().status, equalTo(TaskStatus.working))
            assertThat(receivedTask.get().statusMessage, equalTo("Client processing..."))
            assertThat(receivedMeta.get().progressToken, equalTo("server-token" as Any))
        }
    }

    @Test
    fun `can subscribe to resource updates`() {
        val resourceUri = Uri.of("test://resource/1")

        val resources = ServerResources(
            Resource.Static(resourceUri, ResourceName.of("test-resource"), "A test resource") bind {
                ResourceResponse(listOf(Resource.Content.Text("content", resourceUri)))
            }
        )

        val tools = ServerTools(
            Tool("trigger-update", "triggers a resource update") bind {
                resources.triggerUpdated(resourceUri)
                Ok(Content.Text("triggered"))
            }
        )

        withMcpServer(tools = tools, resources = resources) {
            val latch = CountDownLatch(1)
            val receivedUpdate = AtomicReference<Boolean>(false)

            resources().subscribe(resourceUri) {
                receivedUpdate.set(true)
                latch.countDown()
            }

            tools().call(ToolName.of("trigger-update"), ToolRequest())

            assertThat(latch.await(5, SECONDS), equalTo(true))
            assertThat(receivedUpdate.get(), equalTo(true))

            resources().unsubscribe(resourceUri)
        }
    }
}
