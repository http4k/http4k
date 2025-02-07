package org.http4k.lens

import org.http4k.core.Request
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.routing.RequestWithContext

typealias RequestLens<T> = BiDiLens<Request, T>

object RequestKey {
    fun <T : Any> required(name: String): RequestLens<T> {
        val meta = Meta(true, "context", ObjectParam, name, null, emptyMap())
        val get: (Request) -> T = { target ->
            @Suppress("UNCHECKED_CAST")
            when (target) {
                is RequestWithContext -> target.context[name] as? T
                else -> null
            } ?: throw LensFailure(Missing(meta), target = target)
        }
        val setter = { value: T, target: Request ->
            when (target) {
                is RequestWithContext -> RequestWithContext(target.delegate, target.context + (name to value))
                else -> RequestWithContext(target, mapOf(name to value))
            }
        }
        return BiDiLens(meta, get, setter)
    }
    fun <T : Any> optional(name: String): RequestLens<T?> {
        val meta = Meta(true, "context", ObjectParam, name, null, emptyMap())
        val get: (Request) -> T? = { target ->
            @Suppress("UNCHECKED_CAST")
            when (target) {
                is RequestWithContext -> target.context[name] as? T
                else -> null
            }
        }
        val setter = { value: T?, target: Request ->
            when (target) {
                is RequestWithContext -> RequestWithContext(target.delegate, target.context + (name to value))
                else -> RequestWithContext(target, mapOf(name to value))
            }
        }
        return BiDiLens(meta, get, setter)
    }
}
