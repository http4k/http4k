/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.protocol.messages

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.stateless.model.ResultType
import org.http4k.ai.mcp.stateless.util.McpJson
import org.junit.jupiter.api.Test

class InputRequestWireTest {

    private val result = McpTool.Call.Response.Result(
        resultType = ResultType.input_required,
        inputRequests = mapOf(
            "q" to McpElicitation.Create(
                McpElicitation.Create.Params.Form("your name?", McpJson.obj())
            )
        ),
        requestState = "state-1"
    )

    @Test
    fun `an input request keeps its wire shape`() {
        assertThat(
            McpJson.asFormatString(result),
            equalTo(
                """{"resultType":"input_required","inputRequests":{"q":{"params":{"mode":"form",""" +
                    """"message":"your name?","requestedSchema":{}},"method":"elicitation/create"}},""" +
                    """"requestState":"state-1","_meta":{}}"""
            )
        )
    }
}
