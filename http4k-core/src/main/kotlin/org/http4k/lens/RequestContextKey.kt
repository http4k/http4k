package org.http4k.lens

import org.http4k.core.Request
import org.http4k.core.RequestContext
import org.http4k.core.Store
import org.http4k.lens.ParamMeta.ObjectParam
import java.util.UUID

typealias RequestContextLens<T> = BiDiLens<Request, T>

object RequestContextKey {
    fun <T> required(store: Store<RequestContext>, name: String = UUID.randomUUID().toString()): RequestContextLens<T> {
        val meta = Meta(true, "context", ObjectParam, name)
        val get: (Request) -> T = { target ->
            store[target].let {
                val value: T? = it[name]
                value ?: throw LensFailure(Missing(meta), target = it)
            }
        }
        val setter = { value: T, target: Request -> store[target][name] = value; target }
        return BiDiLens(meta, get, setter)
    }

    fun <T : Any?> optional(store: Store<RequestContext>, name: String = UUID.randomUUID().toString()) =
        BiDiLens(Meta(false, "context", ObjectParam, name), { target -> store[target][name] },
            { value: T?, target: Request -> store[target][name] = value; target }
        )

    fun <T : Any?> defaulted(store: Store<RequestContext>, default: T, name: String = UUID.randomUUID().toString()) =
        BiDiLens(Meta(false, "context", ObjectParam, name), { target -> store[target][name] ?: default },
            { value: T, target: Request -> store[target][name] = value; target }
        )
}
