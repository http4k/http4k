package org.http4k.cloudnative.health

import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.format.Json

/**
 * Renders the results of a readiness check into
 */
interface ReadinessCheckResultRenderer : (ReadinessCheckResult) -> String {
    val contentType: ContentType
}

object DefaultReadinessCheckResultRenderer : ReadinessCheckResultRenderer {
    override fun invoke(p1: ReadinessCheckResult) = (listOf(p1) + p1).joinToString("\n") {
        val base = it.name + "=" + it.pass
        when (it) {
            is Failed -> base + " [${it.cause.message}]"
            else -> base
        }
    }

    override val contentType = TEXT_PLAIN
}

object JsonReadinessCheckResultRenderer {
    operator fun <NODE> invoke(json: Json<NODE>): (ReadinessCheckResult) -> String {
        fun render(result: ReadinessCheckResult) = json {
            val core = listOf(
                "name" to string(result.name),
                "success" to boolean(result.pass)
            )
            when (result) {
                is Failed -> core + ("message" to string("${result.cause.message}"))
                else -> core
            }
        }
        return {
            json {
                val children = it.map(::render).map { obj(it) }
                val root = render(it)
                pretty(obj(if (children.isEmpty()) root else root + ("children" to array(children))))
            }
        }
    }
}