package org.http4k.mcp.model

import org.http4k.lens.BiDiLens
import org.http4k.lens.BiDiLensSpec
import org.http4k.lens.LensGet
import org.http4k.lens.LensSet
import org.http4k.lens.ParamMeta.StringParam
import org.http4k.mcp.PromptRequest

/**
 * Spec of a Prompt capability.
 */
class Prompt private constructor(
    val name: String,
    val description: String?,
    val args: List<BiDiLens<PromptRequest, *>>
) : CapabilitySpec {
    constructor(name: String, description: String, vararg args: BiDiLens<PromptRequest, *>) :
        this(name, description, args.toList())

    object Arg : BiDiLensSpec<PromptRequest, String>("promptRequest", StringParam,
        LensGet { name, target -> listOfNotNull(target[name]?.toString()) },
        LensSet { name, values, target -> values.fold(target) { m, v -> m.copy(args = m + (name to v)) } }
    )
}
