package org.http4k.ai.mcp.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import com.natpryce.hamkrest.present
import dev.forkhandles.result4k.orThrow
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
import org.http4k.ai.mcp.model.ElicitationId
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.model.ResourceUriTemplate
import org.http4k.ai.mcp.model.Task
import org.http4k.ai.mcp.model.TaskId
import org.http4k.ai.mcp.model.TaskStatus
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.protocol.messages.McpElicitations
import org.http4k.ai.mcp.server.capability.ServerCompletions
import org.http4k.ai.mcp.server.capability.ServerPrompts
import org.http4k.ai.mcp.server.capability.ServerResources
import org.http4k.ai.mcp.server.capability.ServerTasks
import org.http4k.ai.mcp.server.capability.ServerTools
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.protocol.Sessions
import org.http4k.ai.mcp.server.sessions.SessionEventStore.Companion.InMemory
import org.http4k.ai.mcp.server.sessions.SessionEventTracking
import org.http4k.ai.mcp.server.sessions.SessionProvider
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
import kotlin.random.Random

abstract class McpClientContract<T> : PortBasedTest {

    val clientName get() = McpEntity.of("foobar")

    abstract val doesNotifications: Boolean

    abstract fun clientSessions(): Sessions<T>

    val sessionEventStore = InMemory(100)
    val sessionEventTracking = SessionEventTracking.InMemory()
    val sessionProvider = SessionProvider.Random(Random(0))

    fun withMcpServer(
        tools: ServerTools = ServerTools(),
        resources: ServerResources = ServerResources(),
        prompts: ServerPrompts = ServerPrompts(),
        completions: ServerCompletions = ServerCompletions(),
        tasks: ServerTasks = ServerTasks(),
        test: McpClient.() -> Unit
    ) {
        val protocol = McpProtocol(
            ServerMetaData(McpEntity.of("David"), Version.of("0.0.1")),
            clientSessions(),
            tools = tools,
            resources = resources,
            tasks = tasks,
            prompts = prompts,
            completions = completions
        )

        val server = toPolyHandler(protocol).asServer(JettyLoom(0)).start()
        val mcpClient = clientFor(server.port())

        try {
            mcpClient.start(Duration.ofSeconds(1))
            mcpClient.test()
        } finally {
            mcpClient.stop()
            server.stop()
        }
    }

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

        val resources = ServerResources(
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
        )
        val prompts = ServerPrompts(Prompt(PromptName.of("prompt"), "description1") bind {
            PromptResponse(listOf(Message(Assistant, Content.Text(it.toString()))), "description")
        })
        val completions = ServerCompletions(Reference.ResourceTemplate(Uri.of("https://http4k.org")) bind {
            CompletionResponse(listOf("1", "2"))
        })

        withMcpServer(tools, resources, prompts, completions) {
            val latch = CountDownLatch(1)

            if (doesNotifications) {
                tools().onChange {
                    latch.countDown()
                }
            }

            assertThat(prompts().list().valueOrNull()!!.size, equalTo(1))

            assertThat(
                prompts().get(PromptName.of("prompt"), PromptRequest(mapOf("a1" to "foo")))
                    .valueOrNull()!!.description,
                equalTo("description")
            )

            assertThat(
                resources().list().valueOrNull()!!.size,
                equalTo(1)
            )

            assertThat(
                resources().listTemplates().valueOrNull()!!.size,
                equalTo(1)
            )

            assertThat(
                resources().read(ResourceRequest(Uri.of("https://http4k.org"))).valueOrNull()!!,
                equalTo(ResourceResponse(listOf(Resource.Content.Text("foo", Uri.of("")))))
            )

            assertThat(
                completions()
                    .complete(
                        Reference.ResourceTemplate(Uri.of("https://http4k.org")),
                        CompletionRequest(CompletionArgument("foo", "bar"))
                    ).valueOrNull()!!,
                equalTo(CompletionResponse(listOf("1", "2")))
            )

            assertThat(tools().list().valueOrNull()!!.size, equalTo(2))

            assertThat(
                tools().call(ToolName.of("reverse"), ToolRequest().with(toolArg of "foobar")).valueOrNull()!!,
                equalTo(ToolResponse.Ok(listOf(Content.Text("raboof"))))
            )

            assertThat(
                tools().call(ToolName.of("reverseStructured"), ToolRequest().with(toolArg of "foobar"))
                    .valueOrNull()!!,
                equalTo(ToolResponse.Ok(listOf(Content.Text("""{"foo":"raboof"}""")), obj("foo" to string("raboof"))))
            )

            if (doesNotifications) {
                tools.items = emptyList()

                require(latch.await(2, SECONDS))

                assertThat(tools().list().valueOrNull()!!.size, equalTo(0))
            }
        }

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
                it.client.updateTask(workingTask)
                ToolResponse.Ok(Content.Text("started"))
            },
            Tool("complete-task", "completes a task") bind {
                it.client.updateTask(completedTask)
                it.client.storeTaskResult(taskId, expectedResult)
                ToolResponse.Ok(Content.Text("completed"))
            }
        )

        withMcpServer(tools = tools) {
            tools().call(ToolName.of("start-task"), ToolRequest(meta = Meta("tasks")))

            val tasks = tasks().list().valueOrNull()
            assertThat(tasks?.any { it.taskId == taskId }, equalTo(true))

            val retrieved = tasks().get(taskId).valueOrNull()
            assertThat(retrieved?.taskId, equalTo(taskId))
            assertThat(retrieved?.status, equalTo(TaskStatus.working))

            tools().call(ToolName.of("complete-task"), ToolRequest(meta = Meta("tasks")))

            val result = tasks().result(taskId).valueOrNull()
            assertThat(result, equalTo(expectedResult))

            val cancelResult = tasks().cancel(taskId)
            assertThat(cancelResult.valueOrNull(), equalTo(Unit))

            assertThat(tasks().get(taskId).valueOrNull(), equalTo(null))
        }
    }

    @Test
    fun `tool can return error response`() {
        val toolArg = Tool.Arg.string().required("name")
        val tools = ServerTools(
            Tool("failing", "description", toolArg) bind { error("bad things") }
        )

        withMcpServer(tools = tools) {
            val actual = tools().call(ToolName.of("failing"), ToolRequest().with(toolArg of "boom"))
                .valueOrNull()

            assertThat(actual, present(isA<ToolResponse.Error>()))
        }
    }

    @Test
    fun `tool can return ElicitationRequired response`() {
        val elicitationId = ElicitationId.of("test-elicitation-123")
        val elicitationUrl = Uri.of("https://example.com/auth")

        val elicitationRequired = ToolResponse.ElicitationRequired(
            elicitations = listOf(
                McpElicitations.Request.Url(
                    message = "Please authorize access",
                    url = elicitationUrl,
                    elicitationId = elicitationId
                )
            ),
            message = "Authorization required"
        )

        val tools = ServerTools(
            Tool("needs-auth", "tool that requires authorization") bind {
                elicitationRequired
            }
        )

        withMcpServer(tools = tools) {
            val result = tools().call(ToolName.of("needs-auth"), ToolRequest()).orThrow { Exception(it.toString()) }
            assertThat(result, equalTo(elicitationRequired))
        }
    }

    abstract fun toPolyHandler(protocol: McpProtocol<T>): PolyHandler

    abstract fun clientFor(port: Int): McpClient
}
