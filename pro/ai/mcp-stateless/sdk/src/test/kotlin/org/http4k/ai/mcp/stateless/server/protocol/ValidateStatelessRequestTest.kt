/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.server.protocol

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.stateless.model.McpEntity
import org.http4k.ai.mcp.stateless.model.Meta
import org.http4k.ai.mcp.stateless.model.PromptName
import org.http4k.ai.mcp.stateless.model.TaskId
import org.http4k.ai.mcp.stateless.protocol.ClientCapabilities
import org.http4k.ai.mcp.stateless.protocol.ProtocolVersion
import org.http4k.ai.mcp.stateless.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.stateless.protocol.ServerMetaData
import org.http4k.ai.mcp.stateless.protocol.Version
import org.http4k.ai.mcp.stateless.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.ai.mcp.stateless.protocol.messages.McpPrompt
import org.http4k.ai.mcp.stateless.protocol.messages.McpResource
import org.http4k.ai.mcp.stateless.protocol.messages.McpTask
import org.http4k.ai.mcp.stateless.protocol.messages.McpTool
import org.http4k.ai.mcp.stateless.util.McpJson
import org.http4k.ai.model.ToolName
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.format.MoshiObject
import org.http4k.lens.stateless.MetaKey
import org.http4k.lens.stateless.clientCapabilities
import org.http4k.lens.stateless.protocolVersion
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class ValidateStatelessRequestTest {

    private val metadata =
        ServerMetaData(McpEntity.of("asdasd"), Version.of("123"), protocolVersions = setOf(LATEST_VERSION))

    private val v = LATEST_VERSION.value

    private fun meta(version: ProtocolVersion? = LATEST_VERSION, caps: ClientCapabilities? = ClientCapabilities()): Meta {
        var m = Meta.default
        version?.let { m = MetaKey.protocolVersion().toLens()(it, m) }
        caps?.let { m = MetaKey.clientCapabilities().toLens()(it, m) }
        return m
    }

    private val taskId = TaskId.of("786512e2-9e0d-44bd-8f29-789f320fe840")

    private fun message(meta: Meta = meta()) = McpTool.List.Request(McpTool.List.Request.Params(_meta = meta), "1")

    private fun taskMessage(meta: Meta = meta()) =
        McpTask.Get.Request(McpTask.Get.Request.Params(taskId, _meta = meta), "1")

    private fun promptMessage(meta: Meta = meta()) =
        McpPrompt.Get.Request(McpPrompt.Get.Request.Params(PromptName.of("my_prompt"), _meta = meta), "1")

    private fun resourceMessage(uri: String = "https://example.com/thing.txt", meta: Meta = meta()) =
        McpResource.Read.Request(McpResource.Read.Request.Params(Uri.of(uri), _meta = meta), "1")

    private fun callMessage(meta: Meta = meta()) =
        McpTool.Call.Request(McpTool.Call.Request.Params(name = ToolName.of("my_tool"), _meta = meta), "1")

    private fun request(headerVersion: String? = v, method: String? = "tools/list", name: String? = null) =
        Request(POST, "/mcp")
            .let { r -> headerVersion?.let { r.header("mcp-protocol-version", it) } ?: r }
            .let { r -> method?.let { r.header("mcp-method", it) } ?: r }
            .let { r -> name?.let { r.header("mcp-name", it) } ?: r }

    private fun McpJsonRpcErrorResponse?.code() =
        (this?.error as? MoshiObject)?.get("code")?.let { McpJson.integer(it).toInt() }

    @Test
    fun `a fully-described request is valid`() {
        assertThat(message().validate(request(), metadata), absent())
    }

    @Test
    fun `a request omitting clientInfo is still valid`() {
        assertThat(message(meta()).validate(request(), metadata), absent())
    }

    @Test
    fun `missing protocolVersion is rejected -32602`() {
        assertThat(message(meta(version = null)).validate(request(), metadata).code(), equalTo(-32602))
    }

    @Test
    fun `missing clientCapabilities is rejected -32602`() {
        assertThat(message(meta(caps = null)).validate(request(), metadata).code(), equalTo(-32602))
    }

    @Test
    fun `an unsupported version is rejected -32022`() {
        val v999 = ProtocolVersion.of("v999.0.0")
        assertThat(
            message(meta(version = v999)).validate(request("v999.0.0"), metadata).code(),
            equalTo(-32022)
        )
    }

    @Test
    fun `a header that does not match _meta is rejected -32020 even for an unsupported version`() {
        val v999 = ProtocolVersion.of("v999.0.0")
        assertThat(
            message(meta(version = v999)).validate(request(v), metadata).code(),
            equalTo(-32020)
        )
    }

    @Test
    fun `a missing Mcp-Method mirror header is rejected -32020`() {
        assertThat(message().validate(request(method = null), metadata).code(), equalTo(-32020))
    }

    @Test
    fun `a Mcp-Method mirror header that does not match the body method is rejected -32020`() {
        assertThat(message().validate(request(method = "tools/call"), metadata).code(), equalTo(-32020))
    }

    @Test
    fun `a Mcp-Method mirror header differing only in case is rejected -32020`() {
        assertThat(message().validate(request(method = "TOOLS/LIST"), metadata).code(), equalTo(-32020))
    }

    @Test
    fun `an OWS-padded Mcp-Method mirror header is valid`() {
        assertThat(message().validate(request(method = "  tools/list  "), metadata), absent())
    }

    @Test
    fun `a matching Mcp-Name mirror header is valid`() {
        assertThat(
            callMessage().validate(request(method = "tools/call", name = "my_tool"), metadata),
            absent()
        )
    }

    @Test
    fun `an OWS-padded Mcp-Name mirror header is valid`() {
        assertThat(
            callMessage().validate(request(method = "tools/call", name = "  my_tool  "), metadata),
            absent()
        )
    }

    @Test
    fun `a missing Mcp-Name mirror header on a targeted request is rejected -32020`() {
        assertThat(
            callMessage().validate(request(method = "tools/call", name = null), metadata).code(),
            equalTo(-32020)
        )
    }

    @Test
    fun `a Mcp-Name mirror header that does not match the body target is rejected -32020`() {
        assertThat(
            callMessage().validate(request(method = "tools/call", name = "other_tool"), metadata).code(),
            equalTo(-32020)
        )
    }

    @Test
    fun `a prompts-get request mirroring its prompt name in Mcp-Name is valid`() {
        assertThat(
            promptMessage().validate(request(method = "prompts/get", name = "my_prompt"), metadata),
            absent()
        )
    }

    @Test
    fun `a prompts-get request whose Mcp-Name is not the prompt name is rejected`(approver: Approver) {
        approver.assertApproved(
            promptMessage().validate(request(method = "prompts/get", name = "other_prompt"), metadata),
        )
    }

    @Test
    fun `a resources-read request mirroring its uri in Mcp-Name is valid`() {
        assertThat(
            resourceMessage().validate(request(method = "resources/read", name = "https://example.com/thing.txt"), metadata),
            absent()
        )
    }

    @Test
    fun `a resources-read request whose Mcp-Name is not the uri is rejected`(approver: Approver) {
        approver.assertApproved(
            resourceMessage().validate(request(method = "resources/read", name = "file:///other.txt"), metadata),
        )
    }

    @Test
    fun `a file uri resource keeps its empty authority when mirrored`() {
        assertThat(
            resourceMessage("file:///thing.txt")
                .validate(request(method = "resources/read", name = "file:///thing.txt"), metadata),
            absent()
        )
    }

    @Test
    fun `a uri with no authority at all is not given one`() {
        assertThat(
            resourceMessage("urn:isbn:1")
                .validate(request(method = "resources/read", name = "urn:isbn:1"), metadata),
            absent()
        )
    }

    @Test
    fun `a task request mirroring its taskId in Mcp-Name is valid`() {
        assertThat(
            taskMessage().validate(request(method = "tasks/get", name = taskId.value), metadata),
            absent()
        )
    }

    @Test
    fun `a task request whose Mcp-Name is not the taskId is rejected`(approver: Approver) {
        approver.assertApproved(
            taskMessage().validate(request(method = "tasks/get", name = "some-other-task"), metadata),
        )
    }

    @Test
    fun `a task request with no Mcp-Name is rejected`(approver: Approver) {
        approver.assertApproved(
            taskMessage().validate(request(method = "tasks/get", name = null), metadata),
        )
    }

    private fun Approver.assertApproved(error: McpJsonRpcErrorResponse?) =
        assertApproved(McpJson.asFormatString(error!!), APPLICATION_JSON)
}
