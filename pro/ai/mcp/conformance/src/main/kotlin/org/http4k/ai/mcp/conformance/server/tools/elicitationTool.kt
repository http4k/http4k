/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.conformance.server.tools

import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.ai.mcp.ElicitationRequest
import org.http4k.ai.mcp.ElicitationResponse
import org.http4k.ai.mcp.ElicitationResponse.Ok
import org.http4k.ai.mcp.ElicitationResponse.Task
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.ToolResponse.Error
import org.http4k.ai.mcp.model.Elicitation
import org.http4k.ai.mcp.model.ElicitationModel
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.string
import org.http4k.format.auto
import org.http4k.lens.MetaKey
import org.http4k.lens.progressToken
import org.http4k.routing.bind

class UserForm : ElicitationModel() {
    val response by string("response", "User's response")
}

val message = Tool.Arg.string().required("message")

val userForm = Elicitation.auto(UserForm()).toLens("form", "it's a form")

fun elicitationTool() = Tool("test_elicitation", "test_elicitation", message) bind {
    it.client.elicit(ElicitationRequest.Form(message(it), userForm, progressToken = MetaKey.progressToken<Any>().toLens()(it.meta)))
        .map {
            when (it) {
                is Ok -> ToolResponse.Ok(it.content.toString())
                is Task -> error("Unexpected task response")
                is ElicitationResponse.Error -> Error(it.message)
            }
        }.mapFailure { Error("Problem with response") }
        .get()
}

