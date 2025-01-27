package org.http4k.lens

import org.http4k.core.Response
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.routing.ResponseWithContext
import java.util.UUID

typealias ResponseLens<T> = BiDiLens<Response, T>

object ResponseKey {
    fun <T : Any> of(name: String = UUID.randomUUID().toString()): ResponseLens<T> {
        val meta = Meta(true, "context", ObjectParam, name, null, emptyMap())
        val get: (Response) -> T = { target ->
            @Suppress("UNCHECKED_CAST")
            when (target) {
                is ResponseWithContext -> target.context[name] as? T
                else -> null
            } ?: throw LensFailure(Missing(meta), target = target)
        }
        val setter = { value: T, target: Response ->
            when (target) {
                is ResponseWithContext -> ResponseWithContext(target.delegate, target.context + (name to value))
                else -> ResponseWithContext(target, mapOf(name to value))
            }
        }
        return BiDiLens(meta, get, setter)
    }
}
