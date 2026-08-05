/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.conformance.server.tools

import org.http4k.ai.mcp.ElicitationRequest
import org.http4k.ai.mcp.ElicitationResponse
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.util.McpJson
import org.http4k.lens.MetaKey
import org.http4k.lens.clientCapabilities
import org.http4k.routing.bind

internal fun elicitationSchema(field: String, type: String = "string") = McpJson {
    obj(
        "type" to string("object"),
        "properties" to obj(field to obj("type" to string(type))),
        "required" to array(string(field))
    )
}

fun inputRequiredResultElicitationTool() =
    Tool("test_input_required_result_elicitation", "test_input_required_result_elicitation") bind { req ->
        when (val answer = req.inputResponses["user_name"]) {
            is ElicitationResponse.Ok -> ToolResponse.Ok("Hello, ${answer.content}")

            else -> ToolResponse.InputRequired(
                mapOf("user_name" to ElicitationRequest.Form("What is your name?", elicitationSchema("name")))
            )
        }
    }

fun inputRequiredResultRequestStateTool() =
    Tool("test_input_required_result_request_state", "test_input_required_result_request_state") bind { req ->
        when (req.inputResponses["confirm"]) {
            is ElicitationResponse.Ok -> ToolResponse.Ok("state-ok: ${req.requestState}")

            else -> ToolResponse.InputRequired(
                mapOf("confirm" to ElicitationRequest.Form("Confirm?", elicitationSchema("ok", "boolean"))),
                requestState = "request-state-token"
            )
        }
    }

fun inputRequiredResultMultiRoundTool() =
    Tool("test_input_required_result_multi_round", "test_input_required_result_multi_round") bind { req ->
        when {
            req.inputResponses["step2"] is ElicitationResponse.Ok -> ToolResponse.Ok("done")

            req.inputResponses["step1"] is ElicitationResponse.Ok -> ToolResponse.InputRequired(
                mapOf("step2" to ElicitationRequest.Form("Pick a colour", elicitationSchema("colour"))),
                requestState = "multi-round-2"
            )

            else -> ToolResponse.InputRequired(
                mapOf("step1" to ElicitationRequest.Form("What is your name?", elicitationSchema("name"))),
                requestState = "multi-round-1"
            )
        }
    }

fun inputRequiredResultTamperedStateTool() =
    Tool("test_input_required_result_tampered_state", "test_input_required_result_tampered_state") bind { req ->
        when (req.inputResponses["confirm"]) {
            is ElicitationResponse.Ok -> ToolResponse.Ok("state-verified")

            else -> ToolResponse.InputRequired(
                mapOf("confirm" to ElicitationRequest.Form("Confirm?", elicitationSchema("ok", "boolean"))),
                requestState = "tampered-state-round-1"
            )
        }
    }

fun inputRequiredResultCapabilitiesTool() =
    Tool("test_input_required_result_capabilities", "test_input_required_result_capabilities") bind { req ->
        val elicitationDeclared = MetaKey.clientCapabilities().toLens()(req.meta)?.elicitation != null
        when {
            req.inputResponses["data"] is ElicitationResponse.Ok -> ToolResponse.Ok("done")

            elicitationDeclared -> ToolResponse.InputRequired(
                mapOf("data" to ElicitationRequest.Form("Provide data", elicitationSchema("data")))
            )

            else -> ToolResponse.InputRequired(emptyMap())
        }
    }
