package org.http4k.ai.mcp.model

import org.http4k.ai.mcp.ElicitationResponse
import org.http4k.ai.mcp.util.McpJson
import org.http4k.format.ConfigurableMoshi
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta.ObjectParam

class ElicitationContentLensSpec<OUT : Any>(private val example: OUT, internal val json: ConfigurableMoshi = McpJson) {
    fun toLens(name: String, description: String) = McpCapabilityLens(
        Meta(true, "elicitationResponse", ObjectParam, name, description, emptyMap()),
        { json.asA(json.asFormatString(it.content), example::class) },
        { out, target: ElicitationResponse -> target.copy(content = json.asJsonObject(out)) },
        {
            json {
                obj(
                    fields(json.asJsonObject(example))
                        .map {
                            it.first to obj(
                                "type" to string(typeOf(it.second).name),
                                "description" to string(typeOf(it.second).name),
                                "title" to string(it.first),
                            )
                        }
                )
            }
        }
    )
}
