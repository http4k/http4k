package org.http4k.ai.mcp.conformance.server.tools

import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.ai.mcp.ElicitationRequest
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Elicitation
import org.http4k.ai.mcp.model.Elicitation.Metadata.EnumMapping
import org.http4k.ai.mcp.model.Elicitation.Metadata.EnumMappings
import org.http4k.ai.mcp.model.ElicitationModel
import org.http4k.ai.mcp.model.Tool
import org.http4k.format.auto
import org.http4k.routing.bind

enum class Options { option1, option2, option3 }
enum class Values { value1, value2, value3 }
enum class Opt { opt1, opt2, opt3 }

class DefaultsForm2 : ElicitationModel() {
    //1. Untitled single-select: { type: "string", enum: ["option1", "option2", "option3"] }
    val untitledSingle by enum("untitledSingle", "", EnumMapping<Options>())

    //2. Titled single-select: { type: "string", oneOf: [{ const: "value1", title: "First Option" }, ...] }
    val titledSingle by enum(
        "titledSingle", "", EnumMapping(
            mappings = mapOf(
                Values.value1 to "First Option",
                Values.value2 to "Second Option",
                Values.value3 to "Third Option"
            )
        )
    )

    //3. Legacy titled (deprecated): { type: "string", enum: ["opt1", "opt2", "opt3"], enumNames: ["Option One", "Option Two", "Option Three"] }
    val legacyEnum by enum(
        "legacyEnum", "", EnumMapping(
            mappings = mapOf(Opt.opt1 to "Option One", Opt.opt2 to "Option Two", Opt.opt3 to "Option Three")
        )
    )

    //4. Untitled multi-select: { type: "array", items: { type: "string", enum: ["option1", "option2", "option3"] } }
    val untitledMulti by enums<Options>("untitledMulti", "")

    //5. Titled multi-select: { type: "array", items: { anyOf: [{ const: "value1", title: "First Choice" }, ...] } }
    val titledMulti by enums(
        "titledMulti", "", EnumMappings(
            mappings = mapOf(
                Values.value1 to "First Choice",
                Values.value2 to "Second Choice",
                Values.value3 to "Third Choice"
            )
        )
    )
}

val defaultForm2 = Elicitation.auto(DefaultsForm2()).toLens("form", "it's a form")

fun elicitationSep1330Enums() = Tool("test_elicitation_sep1330_enums", "test_elicitation_sep1330_enums") bind {
    it.client.elicit(
        ElicitationRequest.Form(
            "Please review and update the form fields with defaults",
            defaultForm2,
            progressToken = it.meta.progressToken
        )
    )
        .map { ToolResponse.Ok(it.content.toString()) }
        .mapFailure { ToolResponse.Error(1, "Problem with response") }
        .get()
}
