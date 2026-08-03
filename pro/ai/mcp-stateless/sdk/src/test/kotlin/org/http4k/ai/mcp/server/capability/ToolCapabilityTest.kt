/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.Client.Companion.NoOp
import org.http4k.ai.mcp.ElicitationRequest
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.enum
import org.http4k.ai.mcp.model.int
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.protocol.messages.MissingRequiredClientCapabilityError
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.asFormatString
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.security.ResponseType
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class ToolCapabilityTest {

    @Test
    fun `can convert to json schema`(approver: Approver) {
        approver.assertApproved(
            asFormatString(
                Tool(
                    "tool", "description",
                    Tool.Arg.string().optional("foo", "bar"),
                    Tool.Arg.int().required("bar", "foo"),
                    Tool.Arg.enum<ResponseType>().required("enum", "foo"),
                    Tool.Arg.string().multi.required("multibar", "foo")
                ).toSchema()
            ), APPLICATION_JSON
        )
    }

    @Test
    fun `tool returning Error passes through content and structuredContent`() {
        val structured = McpJson { obj("error" to string("payment required"), "code" to number(402)) }
        val content = listOf(Text("Payment required"))

        val tool = Tool("paid-tool", "A paid tool")
        val capability = ToolCapability(tool) { ToolResponse.Error(content, structured) }

        val response = capability.call(
            McpTool.Call.Request.Params(tool.name),
            NoOp,
            Request(GET, "/")
        )

        assertThat(response.isError, equalTo(true))
        assertThat(response.content, equalTo(content))
        assertThat(response.structuredContent, equalTo(structured))
    }

    @Test
    fun `structuredContent may be a non-object json value`() {
        val structured = McpJson { array(listOf(number(1), number(2))) }
        val tool = Tool("list-tool", "returns a list")
        val capability = ToolCapability(tool) { ToolResponse.Ok(emptyList(), structured) }

        val response = capability.call(McpTool.Call.Request.Params(tool.name), NoOp, Request(GET, "/"))

        assertThat(response.structuredContent, equalTo(structured))
    }

    @Test
    fun `a domain McpException thrown by the handler propagates unchanged`() {
        val tool = Tool("needs-sampling", "requires sampling")
        val capability = ToolCapability(tool) {
            throw McpException(MissingRequiredClientCapabilityError(listOf("sampling")))
        }

        val e = assertThrows<McpException> {
            capability.call(McpTool.Call.Request.Params(tool.name), NoOp, Request(GET, "/"))
        }

        assertThat(e.error.code, equalTo(MissingRequiredClientCapabilityError.CODE))
    }

    @Test
    fun `elicitation is rejected when the client did not declare the capability`() {
        val tool = Tool("greet", "greets")
        val capability = ToolCapability(tool) {
            ToolResponse.InputRequired(mapOf("login" to ElicitationRequest.Form("please log in")))
        }

        val e = assertThrows<McpException> {
            capability.call(McpTool.Call.Request.Params(tool.name), NoOp, Request(GET, "/"))
        }

        assertThat(e.error.code, equalTo(MissingRequiredClientCapabilityError.CODE))
    }
}
