package org.http4k.lens

import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.lens.ParamMeta.ObjectParam
import java.util.UUID


typealias RequestContextLens<T> = BiDiLens<Request, T>

object RequestContextKey {

    inline fun <reified T : Any> required(contexts: RequestContexts, name: String = UUID.randomUUID().toString()): RequestContextLens<T> {
        val meta = Meta(true, "context", ObjectParam, name)
        return BiDiLens(meta, { target ->
            contexts[target].let {
                it[name] ?: throw LensFailure(Missing(meta), target = it)
            }
        }, { value: T, target: Request -> contexts[target][name] = value; target })
    }

    inline fun <reified T : Any?> optional(contexts: RequestContexts, name: String = UUID.randomUUID().toString()) =
        BiDiLens(Meta(false, "context", ObjectParam, name), { target -> contexts[target][name] },
            { value: T?, target: Request -> contexts[target][name] = value; target }
        )

    inline fun <reified T : Any?> defaulted(contexts: RequestContexts, default: T, name: String = UUID.randomUUID().toString()) =
        BiDiLens(Meta(false, "context", ObjectParam, name), { target -> contexts[target][name] ?: default },
            { value: T, target: Request -> contexts[target][name] = value; target }
        )
}
