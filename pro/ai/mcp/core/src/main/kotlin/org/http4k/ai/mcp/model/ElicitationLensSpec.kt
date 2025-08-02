package org.http4k.ai.mcp.model

import org.http4k.ai.mcp.ElicitationResponse
import org.http4k.ai.mcp.util.McpJson.asJsonObject
import org.http4k.ai.mcp.util.McpJson.obj
import org.http4k.ai.mcp.util.McpJson.string
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiObject
import org.http4k.lens.LensExtractor
import org.http4k.lens.LensGet
import org.http4k.lens.LensSet
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta
import org.http4k.lens.ParamMeta.ObjectParam

open class ElicitationLensSpec<OUT : Any?>(
    internal val paramMeta: ParamMeta,
    private val metadata: Map<String, MoshiNode> = emptyMap(),
    internal val get: LensGet<ElicitationResponse, OUT>,
    internal val set: LensSet<ElicitationResponse, OUT>,
    private val toSchema: McpCapabilityLens<ElicitationResponse, *>.(Map<String, MoshiNode>) -> McpNodeType
) {
    fun <NEXT> map(
        nextIn: (OUT) -> NEXT,
        nextOut: (NEXT) -> OUT
    ) = mapWithNewMeta(nextIn, nextOut, paramMeta, metadata)

    fun <NEXT> mapWithNewMeta(
        nextIn: (OUT) -> NEXT,
        nextOut: (NEXT) -> OUT,
        paramMeta: ParamMeta,
        metadata: Map<String, MoshiNode> = emptyMap()
    ) = ElicitationLensSpec(paramMeta, this.metadata + metadata, get.map(nextIn), set.map(nextOut), toSchema)

    fun required(
        name: String,
        title: String,
        description: String,
        vararg metadata: Elicitation.Metadata<OUT, *>
    ): McpCapabilityLens<ElicitationResponse, OUT> {
        val meta = metaFor(true, name, title, description, metadata)

        val getLens = get(name)
        val setLens = set(name)
        return McpCapabilityLens(
            meta, { getLens(it).first() },
            { out: OUT?, target -> setLens(out?.let { listOf(it) } ?: emptyList(), target) },
            meta.toJsonSchema()
        )
    }

    fun optional(
        name: String,
        title: String,
        description: String,
        vararg metadata: Elicitation.Metadata<OUT, *>
    ): McpCapabilityLens<ElicitationResponse, OUT?> {
        val meta = metaFor(false, name, title, description, metadata)

        val getLens = get(name)
        val setLens = set(name)
        return McpCapabilityLens(
            meta, { getLens(it).firstOrNull() },
            { out: OUT?, target -> setLens(out?.let { listOf(it) } ?: emptyList(), target) },
            meta.toJsonSchema()
        )
    }

    fun defaulted(
        name: String,
        default: OUT,
        title: String,
        description: String,
        vararg metadata: Elicitation.Metadata<OUT, *>
    ): McpCapabilityLens<ElicitationResponse, OUT> = defaulted(
        name,
        { default },
        title, description, *metadata
    )

    fun defaulted(
        name: String,
        default: LensExtractor<ElicitationResponse, OUT>,
        title: String,
        description: String,
        vararg metadata: Elicitation.Metadata<OUT, *>
    ): McpCapabilityLens<ElicitationResponse, OUT> {
        val meta = metaFor(false, name, title, description, metadata)
        val getLens = get(name)
        val setLens = set(name)
        return McpCapabilityLens(
            meta,
            { it: ElicitationResponse -> getLens(it).run { if (isEmpty()) default(it) else first() } },
            { out: OUT, target: ElicitationResponse -> setLens(out?.let { listOf(it) } ?: emptyList(), target) },
            meta.toJsonSchema()
        )
    }

    private fun Meta.toJsonSchema(): (McpCapabilityLens<ElicitationResponse, *>) -> McpNodeType = {
        toSchema(it, metadata.mapValues { it.value as MoshiNode })
    }

    protected fun metaFor(
        required: Boolean,
        name: String,
        title: String,
        description: String,
        metadata: Array<out Elicitation.Metadata<OUT, *>>
    ) = Meta(
        required, "elicitationResponse", paramMeta, name, description,
        (metadata
            .flatMap { it.data().map { it.first to asJsonObject(it.second) } } +
            this.metadata.map { it.key to asJsonObject(it.value) } +
            ("type" to string(paramMeta.description)) +
            ("title" to string(title)) +
            ("description" to string(description))).toMap()
    )

    companion object : ElicitationLensSpec<MoshiNode>(
        ObjectParam,
        emptyMap(),
        LensGet { name, target ->
            listOfNotNull((target.content as MoshiObject)[name])
        },
        LensSet { name: String, values, target ->
            values.fold(target) { m, v -> m.copy(content = asJsonObject(v)) }
        }, { obj(it.toList()) })
}
