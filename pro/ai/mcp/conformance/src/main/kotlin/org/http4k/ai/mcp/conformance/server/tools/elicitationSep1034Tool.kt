package org.http4k.ai.mcp.conformance.server.tools

import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.ai.mcp.ElicitationRequest
import org.http4k.ai.mcp.ElicitationResponse.Ok
import org.http4k.ai.mcp.ElicitationResponse.Task
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.conformance.server.tools.Status.active
import org.http4k.ai.mcp.model.Elicitation
import org.http4k.ai.mcp.model.ElicitationModel
import org.http4k.ai.mcp.model.Tool
import org.http4k.format.auto
import org.http4k.routing.bind

enum class Status { active, inactive, pending }

class DefaultsForm : ElicitationModel() {
    val name by string("name", "User name", "John Doe")
    val age by int("age", "User age", 30)
    val score by double("score", "User score", 95.5)
    val status by enum("status", "User status", default = active)
    val verified by boolean("verified", "Verification status", true)
}

val defaultForm = Elicitation.auto(DefaultsForm()).toLens("form", "it's a form")

fun elicitationSep1034Tool() = Tool("test_elicitation_sep1034_defaults", "test_elicitation_sep1034_defaults") bind {
    it.client.elicit(
        ElicitationRequest.Form(
            "Please review and update the form fields with defaults",
            defaultForm,
            progressToken = it.meta.progressToken
        )
    )
        .map {
            when (it) {
                is Ok -> ToolResponse.Ok(it.content.toString())
                is Task -> error("Unexpected task response")
            }
        }
        .mapFailure { ToolResponse.Error("Problem with response") }
        .get()
}
