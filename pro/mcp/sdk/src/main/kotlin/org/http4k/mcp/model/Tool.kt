package org.http4k.mcp.model

import org.http4k.lens.BiDiLens
import org.http4k.lens.BiDiLensSpec
import org.http4k.lens.LensGet
import org.http4k.lens.LensSet
import org.http4k.lens.ParamMeta.StringParam
import org.http4k.mcp.ToolRequest

class Tool private constructor(
    val name: String,
    val description: String,
    val args: List<BiDiLens<ToolRequest, *>>
) {
    constructor(name: String, description: String, vararg arguments: BiDiLens<ToolRequest, *>) : this(
        name,
        description,
        arguments.toList()
    )

    object Arg : BiDiLensSpec<ToolRequest, String>("toolRequest", StringParam,
        LensGet { name, target -> listOfNotNull(target.args[name]?.toString()) },
        LensSet { name, values, target -> values.fold(target) { m, v -> m.copy(args = m.args + (name to v)) } }
    )
}

