package org.http4k.lens

import org.http4k.core.Response
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.routing.ResponseWithContext

typealias ResponseLens<T> = BiDiLens<Response, T>

object ResponseKey {

    /**
     * Represents a mandatory value in the context of a Response.
     */
    fun <T : Any> required(name: String): ResponseLens<T> {
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

    @Deprecated("use ResponseKey.required", replaceWith = ReplaceWith("ResponseKey.required"))
    fun <T : Any> of(name: String): ResponseLens<T> = required(name)

    /**
     * Represents a nullable value in the context of a Response.
     */
    fun <T : Any> optional(name: String): ResponseLens<T?> {
        val meta = Meta(true, "context", ObjectParam, name, null, emptyMap())
        val get: (Response) -> T? = { target ->
            @Suppress("UNCHECKED_CAST")
            when (target) {
                is ResponseWithContext -> target.context[name] as? T
                else -> null
            }
        }
        val setter = { value: T?, target: Response ->
            val wrappedValue = if (value == null) emptyMap() else mapOf(name to value)
            when (target) {
                is ResponseWithContext -> ResponseWithContext(target.delegate, target.context + wrappedValue)
                else -> ResponseWithContext(target, wrappedValue)
            }
        }
        return BiDiLens(meta, get, setter)
    }
}
