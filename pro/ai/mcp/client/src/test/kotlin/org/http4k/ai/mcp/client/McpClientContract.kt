package org.http4k.ai.mcp.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.CompletionRequest
import org.http4k.ai.mcp.CompletionResponse
import org.http4k.ai.mcp.PromptRequest
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.CompletionArgument
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.model.ResourceUriTemplate
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Task
import org.http4k.ai.mcp.model.TaskId
import org.http4k.ai.mcp.model.TaskStatus
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.capability.ServerCompletions
import org.http4k.ai.mcp.server.capability.ServerPrompts
import org.http4k.ai.mcp.server.capability.ServerResources
import org.http4k.ai.mcp.server.capability.ServerTools
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.protocol.Sessions
import org.http4k.ai.mcp.util.McpJson.auto
import org.http4k.ai.mcp.util.McpJson.obj
import org.http4k.ai.mcp.util.McpJson.string
import org.http4k.ai.model.Role.Companion.Assistant
import org.http4k.ai.model.ToolName
import org.http4k.core.PolyHandler
import org.http4k.core.Uri
import org.http4k.lens.with
import org.http4k.routing.bind
import org.http4k.server.JettyLoom
import org.http4k.server.asServer
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

interface McpClientContract<T> : PortBasedTest {

    val clientName get() = McpEntity.of("foobar")

    val doesNotifications: Boolean

    fun clientSessions(): Sessions<T>

    data class FooBar(val foo: String)

    @Test
    fun `can interact with server`() {

        val toolArg = Tool.Arg.string().required("name")
        val output = Tool.Output.auto(FooBar("bar")).toLens()

        val tools = ServerTools(
            Tool("reverse", "description", toolArg) bind {
                ToolResponse.Ok(listOf(Content.Text(toolArg(it).reversed())))
            },
            Tool("reverseStructured", "description", toolArg) bind {
                ToolResponse.Ok().with(output of FooBar(toolArg(it).reversed()))
            },
        )

        val protocol = McpProtocol(
            ServerMetaData(McpEntity.of("David"), Version.of("0.0.1")),
            clientSessions(),
            tools,
            ServerResources(
                Resource.Static(
                    Uri.of("https://http4k.org"),
                    ResourceName.of("HTTP4K"),
                    "description"
                ) bind {
                    ResourceResponse(listOf(Resource.Content.Text("foo", Uri.of(""))))
                },
                Resource.Templated(
                    ResourceUriTemplate.of("https://http4k.org"),
                    ResourceName.of("HTTP4K"),
                    "templated resource"
                ) bind {
                    ResourceResponse(listOf(Resource.Content.Text("foo", Uri.of(""))))
                }
            ),
            ServerPrompts(Prompt(PromptName.of("prompt"), "description1") bind {
                PromptResponse(listOf(Message(Assistant, Content.Text(it.toString()))), "description")
            }),
            ServerCompletions(Reference.ResourceTemplate(Uri.of("https://http4k.org")) bind {
                CompletionResponse(listOf("1", "2"))
            }),
        )

        val server = toPolyHandler(protocol).asServer(JettyLoom(0)).start()

        val mcpClient = clientFor(server.port())

        val latch = CountDownLatch(1)

        if (doesNotifications) {
            mcpClient.tools().onChange {
                latch.countDown()
            }
        }

        mcpClient.start()

        assertThat(mcpClient.prompts().list().valueOrNull()!!.size, equalTo(1))

        assertThat(
            mcpClient.prompts().get(PromptName.of("prompt"), PromptRequest(mapOf("a1" to "foo")))
                .valueOrNull()!!.description,
            equalTo("description")
        )

        assertThat(
            mcpClient.resources().list().valueOrNull()!!.size,
            equalTo(1)
        )

        assertThat(
            mcpClient.resources().listTemplates().valueOrNull()!!.size,
            equalTo(1)
        )

        assertThat(
            mcpClient.resources().read(ResourceRequest(Uri.of("https://http4k.org"))).valueOrNull()!!,
            equalTo(ResourceResponse(listOf(Resource.Content.Text("foo", Uri.of("")))))
        )

        assertThat(
            mcpClient.completions()
                .complete(
                    Reference.ResourceTemplate(Uri.of("https://http4k.org")),
                    CompletionRequest(CompletionArgument("foo", "bar"))
                ).valueOrNull()!!,
            equalTo(CompletionResponse(listOf("1", "2")))
        )

        assertThat(mcpClient.tools().list().valueOrNull()!!.size, equalTo(2))

        assertThat(
            mcpClient.tools().call(ToolName.of("reverse"), ToolRequest().with(toolArg of "foobar")).valueOrNull()!!,
            equalTo(ToolResponse.Ok(listOf(Content.Text("raboof"))))
        )

        assertThat(
            mcpClient.tools().call(ToolName.of("reverseStructured"), ToolRequest().with(toolArg of "foobar"))
                .valueOrNull()!!,
            equalTo(ToolResponse.Ok(listOf(Content.Text("""{"foo":"raboof"}""")), obj("foo" to string("raboof"))))
        )

        if (doesNotifications) {
            tools.items = emptyList()

            require(latch.await(2, SECONDS))

            assertThat(mcpClient.tools().list().valueOrNull()!!.size, equalTo(0))
        }

        mcpClient.stop()
        server.stop()
    }

    @Test
    fun `task lifecycle - create, list, get, store result, cancel`() {
        val taskId = TaskId.of("lifecycle-task")
        val now = Instant.now()
        val workingTask = Task(taskId, TaskStatus.working, "Processing...", now, now)
        val completedTask = Task(taskId, TaskStatus.completed, "Done", now, now)
        val expectedResult = mapOf("answer" to "42", "status" to "success")

        val tools = ServerTools(
            Tool("start-task", "starts a task") bind {
                it.client.tasks().update(workingTask)
                ToolResponse.Ok(Content.Text("started"))
            },
            Tool("complete-task", "completes a task") bind {
                it.client.tasks().update(completedTask)
                it.client.tasks().storeResult(taskId, expectedResult)
                ToolResponse.Ok(Content.Text("completed"))
            }
        )

        val protocol = McpProtocol(
            ServerMetaData(McpEntity.of("David"), Version.of("0.0.1")),
            clientSessions(),
            tools = tools
        )

        val server = toPolyHandler(protocol).asServer(JettyLoom(0)).start()
        val mcpClient = clientFor(server.port())

        mcpClient.start(Duration.ofSeconds(1))

        mcpClient.tools().call(ToolName.of("start-task"), ToolRequest(meta = Meta("tasks")))

        val tasks = mcpClient.tasks().list().valueOrNull()
        assertThat(tasks?.any { it.taskId == taskId }, equalTo(true))

        val retrieved = mcpClient.tasks().get(taskId).valueOrNull()
        assertThat(retrieved?.taskId, equalTo(taskId))
        assertThat(retrieved?.status, equalTo(TaskStatus.working))

        mcpClient.tools().call(ToolName.of("complete-task"), ToolRequest(meta = Meta("tasks")))

        val result = mcpClient.tasks().result(taskId).valueOrNull()
        assertThat(result, equalTo(expectedResult))

        val cancelResult = mcpClient.tasks().cancel(taskId)
        assertThat(cancelResult.valueOrNull(), equalTo(Unit))

        assertThat(mcpClient.tasks().get(taskId).valueOrNull(), equalTo(null))

        mcpClient.stop()
        server.stop()
    }

    fun toPolyHandler(protocol: McpProtocol<T>): PolyHandler

    fun clientFor(port: Int): McpClient
}
