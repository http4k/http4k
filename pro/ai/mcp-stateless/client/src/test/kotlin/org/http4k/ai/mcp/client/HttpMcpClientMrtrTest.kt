/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.ElicitationRequest
import org.http4k.ai.mcp.ElicitationResponse
import org.http4k.ai.mcp.PromptRequest
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.client.http.HttpMcpClient
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.ElicitationAction
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.model.Role.Companion.Assistant
import org.http4k.ai.model.ToolName
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.http4k.routing.mcp
import org.junit.jupiter.api.Test

class HttpMcpClientMrtrTest {

    private val name = Tool.Arg.string().required("name")

    private val tool = Tool("greet", "greets", name) bind { req ->
        when (req.inputResponses["login"]) {
            is ElicitationResponse.Ok -> ToolResponse.Ok("hi ${name(req)} [state=${req.requestState}]")

            else -> ToolResponse.InputRequired(
                inputRequests = mapOf("login" to ElicitationRequest.Form("please log in")),
                requestState = "s1"
            )
        }
    }

    private val prompt = Prompt(PromptName.of("greet"), "greets") bind { req ->
        when (req.inputResponses["login"]) {
            is ElicitationResponse.Ok -> PromptResponse.Ok(Assistant, "hi [state=${req.requestState}]")

            else -> PromptResponse.InputRequired(
                inputRequests = mapOf("login" to ElicitationRequest.Form("please log in")),
                requestState = "s2"
            )
        }
    }

    private val resource = Resource.Static(Uri.of("res://greet"), ResourceName.of("greet")) bind { req ->
        when (req.inputResponses["login"]) {
            is ElicitationResponse.Ok -> ResourceResponse.Ok(Resource.Content.Text("hi [state=${req.requestState}]", req.uri))

            else -> ResourceResponse.InputRequired(
                inputRequests = mapOf("login" to ElicitationRequest.Form("please log in")),
                requestState = "s3"
            )
        }
    }

    private val server = mcp(ServerMetaData("greeter", "1.0.0"), NoMcpSecurity, tool, prompt, resource)

    private val client = HttpMcpClient(Uri.of("/mcp"), http = server.http!!)

    private val login = mapOf("login" to ElicitationResponse.Ok(ElicitationAction.accept))

    @Test
    fun `caller drives the tools-call elicitation round-trip`() {
        val request = ToolRequest(mapOf("name" to "bob"))

        val first = client.tools().call(ToolName.of("greet"), request).valueOrNull() as ToolResponse.InputRequired
        assertThat(first.inputRequests, equalTo(mapOf("login" to ElicitationRequest.Form("please log in"))))
        assertThat(first.requestState, equalTo("s1"))

        val retried = client.tools().call(
            ToolName.of("greet"), request.copy(inputResponses = login, requestState = first.requestState)
        )
        assertThat((retried.valueOrNull() as ToolResponse.Ok).content, equalTo(listOf(Text("hi bob [state=s1]"))))
    }

    @Test
    fun `caller drives the prompts-get elicitation round-trip`() {
        val first = client.prompts().get(PromptName.of("greet"), PromptRequest()).valueOrNull() as PromptResponse.InputRequired
        assertThat(first.inputRequests, equalTo(mapOf("login" to ElicitationRequest.Form("please log in"))))
        assertThat(first.requestState, equalTo("s2"))

        val retried = client.prompts().get(
            PromptName.of("greet"), PromptRequest(inputResponses = login, requestState = first.requestState)
        )
        assertThat(
            (retried.valueOrNull() as PromptResponse.Ok).messages,
            equalTo(listOf(Message(Assistant, Text("hi [state=s2]"))))
        )
    }

    @Test
    fun `caller drives the resources-read elicitation round-trip`() {
        val uri = Uri.of("res://greet")

        val first = client.resources().read(ResourceRequest(uri)).valueOrNull() as ResourceResponse.InputRequired
        assertThat(first.inputRequests, equalTo(mapOf("login" to ElicitationRequest.Form("please log in"))))
        assertThat(first.requestState, equalTo("s3"))

        val retried = client.resources().read(
            ResourceRequest(uri, inputResponses = login, requestState = first.requestState)
        )
        assertThat(
            (retried.valueOrNull() as ResourceResponse.Ok).list,
            equalTo(listOf(Resource.Content.Text("hi [state=s3]", uri)))
        )
    }
}
