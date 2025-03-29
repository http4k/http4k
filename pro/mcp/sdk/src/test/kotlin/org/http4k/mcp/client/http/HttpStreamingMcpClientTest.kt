package org.http4k.mcp.client.http

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import com.natpryce.hamkrest.present
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.orThrow
import dev.forkhandles.result4k.valueOrNull
import org.http4k.client.JavaHttpClient
import org.http4k.client.ReconnectionMode.Disconnect
import org.http4k.client.chunkedSseSequence
import org.http4k.connect.model.MaxTokens
import org.http4k.connect.model.ModelName
import org.http4k.connect.model.Role.Companion.Assistant
import org.http4k.connect.model.StopReason
import org.http4k.connect.model.ToolName
import org.http4k.core.BodyMode.Stream
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.LAST_EVENT_ID
import org.http4k.lens.MCP_SESSION_ID
import org.http4k.lens.accept
import org.http4k.lens.with
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.PromptResponse
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.SamplingResponse
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.client.McpClientContract
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.Message
import org.http4k.mcp.model.Progress
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.model.PromptName
import org.http4k.mcp.model.Reference
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.ResourceName
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.server.capability.ServerCompletions
import org.http4k.mcp.server.capability.ServerPrompts
import org.http4k.mcp.server.capability.ServerResources
import org.http4k.mcp.server.capability.ServerTools
import org.http4k.mcp.server.firstDeterministicSessionId
import org.http4k.mcp.server.http.HttpStreamingMcp
import org.http4k.mcp.server.http.HttpStreamingSessions
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.protocol.Session
import org.http4k.mcp.server.sessions.SessionEventStore
import org.http4k.mcp.server.sessions.SessionProvider
import org.http4k.routing.bind
import org.http4k.server.Helidon
import org.http4k.server.asServer
import org.http4k.sse.Sse
import org.http4k.sse.SseEventId
import org.http4k.sse.SseMessage
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

class HttpStreamingMcpClientTest : McpClientContract<Sse> {

    override val doesNotifications = true

    override fun clientFor(port: Int) = HttpStreamingMcpClient(
        clientName, Version.of("1.0.0"),
        Uri.of("http://localhost:${port}/mcp"),
        JavaHttpClient(responseBodyMode = Stream),
        ClientCapabilities(),
        notificationSseReconnectionMode = Disconnect,
    )

    override fun clientSessions() = HttpStreamingSessions().apply { start() }

    override fun toPolyHandler(protocol: McpProtocol<Sse>) =
        HttpStreamingMcp(protocol)

    @Test
    fun `deals with error`() {
        val toolArg = Tool.Arg.required("name")

        val protocol = McpProtocol(
            ServerMetaData(McpEntity.of("David"), Version.of("0.0.1")),
            clientSessions(),
            Tool("reverse", "description", toolArg) bind { _ -> error("bad things") }
        )

        val server = toPolyHandler(protocol)
            .asServer(Helidon(0)).start()

        val mcpClient = clientFor(server.port())

        mcpClient.start()

        val actual = mcpClient.tools().call(ToolName.of("reverse"), ToolRequest().with(toolArg of "boom"))
            .valueOrNull()

        assertThat(actual, present(isA<ToolResponse.Error>()))

        mcpClient.stop()
        server.stop()
    }

    @Test
    fun `resume a stream`() {
        val toolArg = Tool.Arg.required("name")
        val tools = ServerTools(Tool("reverse", "description", toolArg) bind { it ->
            ToolResponse.Ok(listOf(Content.Text(toolArg(it).reversed())))
        })

        val protocol = McpProtocol(
            ServerMetaData(McpEntity.of("David"), Version.of("0.0.1")),
            HttpStreamingSessions(
                sessionProvider = SessionProvider.Random(Random(0)),
                eventStore = InMemorySessionEventStore()
            ).apply { start() },
            tools,
            ServerResources(
                Resource.Static(
                    Uri.of("https://http4k.org"),
                    ResourceName.of("HTTP4K"),
                    "description"
                ) bind { _ ->
                    ResourceResponse(listOf(Resource.Content.Text("foo", Uri.of(""))))
                }),
            ServerPrompts(Prompt(PromptName.of("prompt"), "description1") bind { it ->
                PromptResponse(listOf(Message(Assistant, Content.Text(it.toString()))), "description")
            }),
            ServerCompletions(Reference.Resource(Uri.of("https://http4k.org")) bind { it ->
                CompletionResponse(listOf("1", "2"))
            })
        )

        val server = toPolyHandler(protocol).asServer(Helidon(0)).start()

        val mcpClient = clientFor(server.port())

        val latch = CountDownLatch(1)

        mcpClient.start()

        mcpClient.tools().list().orThrow { error("bad things") }

        mcpClient.tools().onChange {
            latch.countDown()
        }

        Thread.sleep(1000) // TODO RACE CONDITION

        tools.items = emptyList()

        require(latch.await(2, SECONDS))

        mcpClient.resources().list().orThrow { error("bad things") }
        mcpClient.prompts().list().orThrow { error("bad things") }

        val messages = JavaHttpClient(responseBodyMode = Stream)(
            Request(GET, Uri.of("http://localhost:${server.port()}/mcp"))
                .accept(TEXT_EVENT_STREAM)
                .with(Header.MCP_SESSION_ID of firstDeterministicSessionId)
                .with(Header.LAST_EVENT_ID of SseEventId("3")),
        ).body.stream.chunkedSseSequence()
            .take(2)
            .filterIsInstance<SseMessage.Event>()
            .map { it.data }
            .toList()

        assertThat(messages.size, equalTo(2))

        mcpClient.stop()
        server.stop()
    }

    @Test
    fun `can do sampling`() {
        val model = ModelName.of("my model")

        val samplingResponses = listOf(
            SamplingResponse(model, Assistant, Content.Text("hello"), null),
            SamplingResponse(model, Assistant, Content.Text("world"), StopReason.of("foobar"))
        )

        val tools = ServerTools(
            Tool("sample", "description") bind { it, client ->
                val received = client.sample(
                    SamplingRequest(listOf(), MaxTokens.of(1), progressToken = it.progressToken!!),
                    Duration.ofSeconds(5)
                ).toList()
                assertThat(received, equalTo(samplingResponses.map { Success(it) }))
                ToolResponse.Ok(listOf(Content.Text(received.size.toString())))
            }
        )

        val eventStore = InMemorySessionEventStore()
        val protocol = McpProtocol(
            ServerMetaData(McpEntity.of("David"), Version.of("0.0.1")),
            HttpStreamingSessions(
                sessionProvider = SessionProvider.Random(Random(0)),
                eventStore = eventStore
            ).apply { start() },
            tools = tools,
        )

        val mcpClient = clientFor(toPolyHandler(protocol).asServer(Helidon(0)).start().port())

        mcpClient.start()

        mcpClient.sampling().onSampled { samplingResponses.asSequence() }

        assertThat(
            mcpClient.tools().call(ToolName.of("sample"), ToolRequest(progressToken = "sample")),
            equalTo(Success(ToolResponse.Ok(Content.Text("2"))))
        )

        assertThat(
            mcpClient.tools().call(ToolName.of("sample"), ToolRequest(progressToken = "sample2")),
            equalTo(Success(ToolResponse.Ok(Content.Text("2"))))
        )

        mcpClient.stop()

        assertThat(eventStore.read(Session(firstDeterministicSessionId), null).toList().size, equalTo(5))
    }

    @Test
    fun `can do progress`() {
        val tools = ServerTools(
            Tool("progress", "description") bind { it, client ->
                try {
                    client.report(Progress(1, 2.0, it.progressToken!!))
                    ToolResponse.Ok(listOf(Content.Text("")))
                } catch (e: Exception) {
                    throw e
                }
            }
        )

        val eventStore = InMemorySessionEventStore()
        val protocol = McpProtocol(
            ServerMetaData(McpEntity.of("David"), Version.of("0.0.1")),
            HttpStreamingSessions(
                sessionProvider = SessionProvider.Random(Random(0)),
                eventStore = eventStore
            ).apply { start() },
            tools = tools
        )

        val mcpClient = clientFor(toPolyHandler(protocol).asServer(Helidon(0)).start().port())

        mcpClient.start()

        val prog = AtomicReference<Progress>()
        mcpClient.progress().onProgress(fn = prog::set)

        assertThat(
            mcpClient.tools().call(ToolName.of("progress"), ToolRequest(progressToken = "progress")),
            equalTo(Success(ToolResponse.Ok(Content.Text(""))))
        )

        assertThat(prog.get(), equalTo(Progress(1, 2.0, "progress")))

        mcpClient.stop()

        val read = eventStore.read(Session(firstDeterministicSessionId), null)
        assertThat(read.toList().size, equalTo(3))
    }
}

private class InMemorySessionEventStore : SessionEventStore {
    private val events = mutableMapOf<Session, ArrayDeque<SseMessage.Event>>()

    override fun read(session: Session, lastEventId: SseEventId?) =
        when (lastEventId) {
            null -> events[session]?.asSequence() ?: emptySequence()

            else -> events[session]
                ?.drop(lastEventId.value.toInt())
                ?.asSequence() ?: emptySequence()
        }

    override fun write(session: Session, message: SseMessage.Event) {
        events.getOrPut(session) { ArrayDeque() }.add(message)
    }
}
