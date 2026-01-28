package org.http4k.ai.mcp.conformance.server.tools

import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.ai.mcp.ElicitationRequest
import org.http4k.ai.mcp.ElicitationResponse
import org.http4k.ai.mcp.ElicitationResponse.Task
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.ToolResponse.Error
import org.http4k.ai.mcp.model.Elicitation
import org.http4k.ai.mcp.model.ElicitationModel
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.string
import org.http4k.format.auto
import org.http4k.routing.bind

class UserForm : ElicitationModel() {
    val response by string("response", "User's response")
}

val message = Tool.Arg.string().required("message")

val userForm = Elicitation.auto(UserForm()).toLens("form", "it's a form")

fun elicitationTool() = Tool("test_elicitation", "test_elicitation", message) bind {
    it.client.elicit(ElicitationRequest.Form(message(it), userForm, progressToken = it.meta.progressToken))
        .map {
            when (it) {
                is ElicitationResponse.Ok -> ToolResponse.Ok(it.content.toString())
                is Task -> error("Unexpected task response")
            }
        }.mapFailure { Error(1, "Problem with response") }
        .get()
}

