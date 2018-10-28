package org.http4k.cloudnative

import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.format.Json

interface ReadinessCheckResultRenderer : (ReadinessCheckResult) -> String {
    val contentType: ContentType
}

object DefaultReadinessCheckResultRenderer : ReadinessCheckResultRenderer {
    override fun invoke(p1: ReadinessCheckResult) = (listOf(p1) + p1).joinToString("\n") { it.name + "=" + it.pass }

    override val contentType = TEXT_PLAIN
}

object JsonReadinessCheckResultRenderer {
    operator fun <NODE> invoke(json: Json<NODE>): (ReadinessCheckResult) -> String {
        fun render(result: ReadinessCheckResult) = result.name to json.boolean(result.pass)
        return {
            val core: List<Pair<String, NODE>> = listOf(it.asJson(json))
            val children = it.map(::render)
            json { pretty(obj(if (children.isEmpty()) core else core + ("children" to obj(children)))) }
        }
    }

    private fun <NODE> ReadinessCheckResult.asJson(json: Json<NODE>) = name to json.boolean(pass)
}