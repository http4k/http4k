package org.http4k.lens

import org.http4k.core.Request
import org.http4k.core.RequestContexts
import java.util.*

typealias RequestContextLens<T> = BiDiLens<Request, T>

object RequestContextKey {
    inline fun <reified T : Any> of(contexts: RequestContexts): RequestContextLens<T> {
        val key = UUID.randomUUID().toString()
        return BiDiLens(Meta(true, "context", ParamMeta.ObjectParam, key), { target -> contexts[target].get(key) },
            { value: T, target: Request -> contexts[target].set(key, value); target }
        )
    }
}
