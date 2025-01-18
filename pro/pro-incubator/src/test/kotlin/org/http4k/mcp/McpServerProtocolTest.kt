package org.http4k.mcp

import com.fasterxml.jackson.databind.JsonNode
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.model.Base64Blob
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.filter.debug
import org.http4k.format.renderNotification
import org.http4k.format.renderRequest
import org.http4k.format.renderResult
import org.http4k.hamkrest.hasStatus
import org.http4k.mcp.features.Prompts
import org.http4k.mcp.features.Resources
import org.http4k.mcp.features.Roots
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.Message
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.Role
import org.http4k.mcp.model.Root
import org.http4k.mcp.protocol.ClientMessage
import org.http4k.mcp.protocol.HasMethod
import org.http4k.mcp.protocol.McpInitialize
import org.http4k.mcp.protocol.McpNotification
import org.http4k.mcp.protocol.McpPrompt
import org.http4k.mcp.protocol.McpRequest
import org.http4k.mcp.protocol.McpResource
import org.http4k.mcp.protocol.McpResponse
import org.http4k.mcp.protocol.McpRoot
import org.http4k.mcp.protocol.ProtocolVersion.Companion.`2024-10-07`
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.server.ClientCapabilities
import org.http4k.mcp.server.McpEntity
import org.http4k.mcp.server.McpHandler
import org.http4k.mcp.server.ServerMetaData
import org.http4k.mcp.util.McpJson
import org.http4k.routing.bind
import org.http4k.sse.SseMessage
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.TestSseClient
import org.http4k.testing.testSseClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.random.Random

@ExtendWith(JsonApprovalTest::class)
class McpServerProtocolTest {

    private val metadata = ServerMetaData(McpEntity("server", Version.of("1")))

    @Test
    fun `performs init loop on startup`() {
        val mcp = McpHandler(metadata, random = Random(0)).debug()

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)
        }
    }

    @Test
    fun `update roots`() {
        val roots = Roots()

        val mcp = McpHandler(metadata, roots = roots, random = Random(0)).debug()

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpRoot.Changed)

            assertNextMessage(McpRoot.List, McpRoot.List.Request(), "a8baf924-2fcc-7be7-020b-7f533060e8ef")

            val newRoots = listOf(Root(Uri.of("asd"), "name"))

            mcp.sendToMcp(McpRoot.List.Response(newRoots), "a8baf924-2fcc-7be7-020b-7f533060e8ef")

            assertThat(roots.toList(), equalTo(newRoots))
        }
    }

    @Test
    fun `deal with prompts`() {
        val prompt = Prompt("prompt", "description", listOf(Prompt.Argument("name", "description", true)))
        val mcp = McpHandler(metadata, prompts = Prompts(
            listOf(
                prompt bind {
                    PromptResponse(
                        "description",
                        listOf(
                            Message(Role.assistant, Content.Text(it.input["name"]!!.reversed()))
                        )
                    )
                }
            )
        ), random = Random(0)).debug()

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpPrompt.List, McpPrompt.List.Request())

            assertNextMessage(McpPrompt.List.Response(listOf(prompt)))

            mcp.sendToMcp(McpPrompt.Get, McpPrompt.Get.Request(prompt.name, mapOf("name" to "value")))

            assertNextMessage(
                McpPrompt.Get.Response(
                    listOf(Message(Role.assistant, Content.Text("eulav"))),
                    "description"
                )
            )
        }
    }

    @Test
    fun `deal with static resources`() {
        val resource = Resource.Static(Uri.of("https://www.http4k.org"), "HTTP4K", "description")
        val content = Resource.Content.Blob(Base64Blob.encode("image"), resource.uri)

        val resources = Resources(listOf(resource bind { ResourceResponse(listOf(content)) }))
        val mcp = McpHandler(metadata, resources = resources, random = Random(0)).debug()

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpResource.List, McpResource.List.Request())

            assertNextMessage(McpResource.List.Response(listOf(resource)))

            mcp.sendToMcp(McpResource.Read, McpResource.Read.Request(resource.uri))

            assertNextMessage(McpResource.Read.Response(listOf(content)))

            mcp.sendToMcp(McpResource.Subscribe, McpResource.Subscribe.Request(resource.uri))

            resources.triggerUpdated(resource.uri)

            assertNextMessage(McpResource.Updated(resource.uri))


        }
    }

    private fun TestSseClient.assertInitializeLoop(mcp: PolyHandler) {
        assertThat(status, equalTo(OK))

        assertThat(
            received().first(),
            equalTo(SseMessage.Event("endpoint", "/message?sessionId=8cb4c22c-53fe-ae50-d94e-97b2a94e6b1e"))
        )

        mcp.sendToMcp(
            McpInitialize, McpInitialize.Request(
                McpEntity("client", Version.of("1")),
                ClientCapabilities(), `2024-10-07`
            )
        )

        assertNextMessage(
            McpInitialize.Response(metadata.entity, metadata.capabilities, metadata.protocolVersion)
        )

        mcp.sendToMcp(McpInitialize.Initialized)
    }
}

private fun TestSseClient.assertNextMessage(input: McpResponse) {
    assertNextMessage(with(McpJson) { renderResult(asJsonObject(input), number(1)) })
}

private fun TestSseClient.assertNextMessage(notification: McpNotification) {
    assertNextMessage(with(McpJson) { renderNotification(notification) })
}

private fun TestSseClient.assertNextMessage(hasMethod: HasMethod, input: McpRequest, id: Any) {
    assertNextMessage(with(McpJson) {
        renderRequest(
            hasMethod.Method.value,
            asJsonObject(input),
            asJsonObject(id)
        )
    })
}

private fun TestSseClient.assertNextMessage(node: JsonNode) {
    assertThat(
        received().first(),
        equalTo(SseMessage.Event("message", with(McpJson) { compact(node) }))
    )
}

private fun PolyHandler.sendToMcp(hasMethod: HasMethod, input: ClientMessage.Request) {
    sendToMcp(with(McpJson) {
        compact(renderRequest(hasMethod.Method.value, asJsonObject(input), number(1)))
    })
}

private fun PolyHandler.sendToMcp(hasMethod: ClientMessage.Response, id: Any) {
    sendToMcp(with(McpJson) {
        compact(renderResult(asJsonObject(hasMethod), asJsonObject(id)))
    })
}

private fun PolyHandler.sendToMcp(hasMethod: ClientMessage.Notification) {
    sendToMcp(with(McpJson) {
        compact(renderNotification(hasMethod))
    })
}

private fun PolyHandler.sendToMcp(body: String) {
    assertThat(
        http!!(
            Request(POST, "/message?sessionId=8cb4c22c-53fe-ae50-d94e-97b2a94e6b1e").body(body)
        ), hasStatus(ACCEPTED)
    )
}
