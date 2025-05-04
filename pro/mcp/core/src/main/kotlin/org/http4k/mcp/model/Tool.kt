package org.http4k.mcp.model

import org.http4k.connect.model.ToolName
import org.http4k.lens.LensGet
import org.http4k.lens.LensSet
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.mcp.ToolRequest

/**
 * Description of a Tool capability.
 */
class Tool private constructor(
    val name: ToolName,
    val description: String,
    val args: List<ToolArgLens<*>>,
    val annotations: ToolAnnotations? = null,
) : CapabilitySpec {
    constructor(
        name: String,
        description: String,
        vararg arguments: ToolArgLens<*>,
        annotations: ToolAnnotations? = null
    ) : this(ToolName.of(name), description, arguments.toList(), annotations)


    /**
     * A typesafe tool argument lens.
     */
    object Arg

    class ArgList<T>(val delegate: List<T>) : List<T> by delegate

}
