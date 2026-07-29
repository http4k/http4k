/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import org.http4k.ai.mcp.model.ElicitationAction
import org.junit.jupiter.api.Test

class ToolMrtrTest {

    private val handler: ToolHandler = { req ->
        when (val login = req.inputResponses["login"]) {
            is ElicitationResponse.Ok ->
                ToolResponse.Ok("welcome ${req.args["name"]} (${login.action}) [state=${req.requestState}]")

            else -> ToolResponse.InputRequired(
                inputRequests = mapOf("login" to ElicitationRequest.Form("Please log in")),
                requestState = "for=${req.args["name"]}"
            )
        }
    }

    @Test
    fun `asks for input when the answer is absent`() {
        val first = handler(ToolRequest(mapOf("name" to "octocat")))

        val inputRequired = first as ToolResponse.InputRequired
        assertThat(inputRequired.inputRequests.keys, equalTo(setOf("login")))
        assertThat(inputRequired.requestState, equalTo("for=octocat"))
    }

    @Test
    fun `completes on retry when the answer is supplied`() {
        val retry = handler(
            ToolRequest(
                args = mapOf("name" to "octocat"),
                inputResponses = mapOf("login" to ElicitationResponse.Ok(ElicitationAction.accept)),
                requestState = "for=octocat"
            )
        )

        assertThat(retry, isA<ToolResponse.Ok>())
    }
}
