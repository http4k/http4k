package org.http4k.ai.mcp.client.http

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.orThrow
import org.http4k.ai.mcp.CompletionResponse
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.client.McpStreamingClientContract
import org.http4k.ai.mcp.firstDeterministicSessionId
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.protocol.ClientCapabilities
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.capability.ServerCompletions
import org.http4k.ai.mcp.server.capability.ServerPrompts
import org.http4k.ai.mcp.server.capability.ServerResources
import org.http4k.ai.mcp.server.capability.ServerTools
import org.http4k.ai.mcp.server.http.HttpSessions
import org.http4k.ai.mcp.server.http.HttpStreamingMcp
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.security.OAuthMcpSecurity
import org.http4k.ai.mcp.server.sessions.SessionProvider
import org.http4k.ai.model.Role.Companion.Assistant
import org.http4k.client.JavaHttpClient
import org.http4k.client.ReconnectionMode.Disconnect
import org.http4k.core.BodyMode.Stream
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.lens.Header
import org.http4k.lens.LAST_EVENT_ID
import org.http4k.lens.MCP_SESSION_ID
import org.http4k.lens.accept
import org.http4k.routing.bind
import org.http4k.server.Helidon
import org.http4k.server.asServer
import org.http4k.sse.Sse
import org.http4k.sse.SseEventId
import org.http4k.sse.SseMessage
import org.http4k.sse.chunkedSseSequence
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.random.Random

@ExtendWith(JsonApprovalTest::class)
class HttpStreamingMcpClientTest : McpStreamingClientContract<Sse>() {

    override val doesNotifications = true

    private val http = ClientFilters.BearerAuth("123").then(
        JavaHttpClient(responseBodyMode = Stream)
    )

    override fun clientFor(port: Int) = HttpStreamingMcpClient(
        clientName, Version.of("1.0.0"),
        Uri.of("http://localhost:${port}/mcp"),
        http,
        ClientCapabilities(),
        notificationSseReconnectionMode = Disconnect,
    )

    override fun clientSessions() = HttpSessions(
        sessionProvider,
        sessionEventTracking,
        sessionEventStore,
    ).apply { start() }

    override fun toPolyHandler(protocol: McpProtocol<Sse>) =
        HttpStreamingMcp(
            protocol, OAuthMcpSecurity(Uri.of("http://auth1"), Uri.of("http://mcp/mcp")) { it == "123" })

    @Test
    fun `can get to auth server details`(approver: Approver) {

        val protocol = McpProtocol(
            ServerMetaData(McpEntity.of("David"), Version.of("0.0.1")),
            clientSessions()
        )

        val server = toPolyHandler(protocol)
            .asServer(Helidon(0)).start()

        val javaHttpClient = JavaHttpClient()
        val message = javaHttpClient(Request(GET, "http://localhost:${server.port()}/mcp"))
        assertThat(message.status, equalTo(UNAUTHORIZED))

        approver.assertApproved(
            javaHttpClient(
                Request(
                    GET,
                    "http://localhost:${server.port()}/.well-known/oauth-protected-resource"
                )
            )
        )
    }

    @Test
    fun `resume a stream`() {
        val toolArg = Tool.Arg.string().required("name")
        val tools = ServerTools(Tool("reverse", "description", toolArg) bind {
            Ok(listOf(Content.Text(toolArg(it).reversed())))
        })

        val protocol = McpProtocol(
            ServerMetaData(McpEntity.of("David"), Version.of("0.0.1")),
            HttpSessions(
                sessionProvider = SessionProvider.Random(Random(0)),
            ).apply { start() },
            tools,
            ServerResources(
                Resource.Static(
                    Uri.of("https://http4k.org"),
                    ResourceName.of("HTTP4K"),
                    "description"
                ) bind {
                    ResourceResponse(listOf(Resource.Content.Text("foo", Uri.of(""))))
                }),
            ServerPrompts(Prompt(PromptName.of("prompt"), "description1") bind {
                PromptResponse(listOf(Message(Assistant, Content.Text(it.toString()))), "description")
            }),
            ServerCompletions(Reference.ResourceTemplate(Uri.of("https://http4k.org")) bind {
                CompletionResponse(listOf("1", "2"))
            })
        )

        val server = toPolyHandler(protocol).asServer(Helidon(0)).start()

        val mcpClient = clientFor(server.port())

        val latch = CountDownLatch(1)

        mcpClient.start(Duration.ofSeconds(1))

        mcpClient.tools().list().orThrow { error("bad things") }

        mcpClient.tools().onChange {
            latch.countDown()
        }

        tools.items = emptyList()

        require(latch.await(2, SECONDS))

        mcpClient.resources().list().orThrow { error("bad things") }
        mcpClient.prompts().list().orThrow { error("bad things") }

        val messages = http(
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
}
