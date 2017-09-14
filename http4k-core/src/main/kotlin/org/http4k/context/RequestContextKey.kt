package org.http4k.context

import org.http4k.core.Request
import org.http4k.lens.BiDiLens
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta
import java.util.*
import kotlin.reflect.KClass

typealias RequestContextLens<T> = BiDiLens<Request, T>

data class RequestContextKey(val id: UUID, val clazz: KClass<*>) {
    companion object {
        inline fun <reified T : Any> of(contexts: RequestContexts): RequestContextLens<T> {
            val key = RequestContextKey(UUID.randomUUID(), T::class)
            return BiDiLens(
                Meta(true, "context", ParamMeta.ObjectParam, key.toString()),
                { target -> contexts[target][key] },
                { value: T, target: Request -> contexts[target][key] = value; target }
            )
        }
    }
}