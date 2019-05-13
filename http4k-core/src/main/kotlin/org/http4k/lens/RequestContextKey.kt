package org.http4k.lens

import org.http4k.core.Request
import org.http4k.core.RequestContext
import org.http4k.core.Store
import org.http4k.lens.ParamMeta.ObjectParam
import java.util.UUID


typealias RequestContextLens<T> = BiDiLens<Request, T>

object RequestContextKey {
    fun <T> required(contexts: Store<RequestContext>, name: String = UUID.randomUUID().toString()): RequestContextLens<T> {
        val meta = Meta(true, "context", ObjectParam, name)
        return BiDiLens(meta, { target ->
            contexts[target].let { it[name] ?: throw LensFailure(Missing(meta), target = it) }
        }, { value: T, target: Request -> contexts[target][name] = value; target })
    }

    fun <T : Any?> optional(contexts: Store<RequestContext>, name: String = UUID.randomUUID().toString()) =
        BiDiLens(Meta(false, "context", ObjectParam, name), { target -> contexts[target][name] },
            { value: T?, target: Request -> contexts[target][name] = value; target }
        )

    fun <T : Any?> defaulted(contexts: Store<RequestContext>, default: T, name: String = UUID.randomUUID().toString()) =
        BiDiLens(Meta(false, "context", ObjectParam, name), { target -> contexts[target][name] ?: default },
            { value: T, target: Request -> contexts[target][name] = value; target }
        )
}
