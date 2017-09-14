package org.http4k.context

import org.http4k.core.Request
import org.http4k.lens.BiDiLens
import org.http4k.lens.Header
import org.http4k.lens.LensExtractor
import org.http4k.lens.LensInjector
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta
import java.util.*
import kotlin.reflect.KClass

typealias RequestContextLens<T> = BiDiLens<Request, T>

object RequestContextKey {
    inline fun <reified T : Any> of(contexts: RequestContexts): RequestContextLens<T> = BiDiLens(
        Meta(true, "context", ParamMeta.ObjectParam, T::class.java.name),
        { target -> contexts[target][T::class] },
        { value: T, target: Request -> contexts[target][T::class] = value; target }
    )
}

class RequestContext(val id: UUID) {
    private val objects = mutableMapOf<KClass<*>, Any>()

    companion object : LensExtractor<Request, UUID>, LensInjector<Request, UUID> {

        private val X_HTTP4K_CONTEXT = Header.map(UUID::fromString, UUID::toString).required("x-http4k-context")

        override fun <R : Request> invoke(value: UUID, target: R): R = X_HTTP4K_CONTEXT(value, target)

        override fun invoke(target: Request): UUID = X_HTTP4K_CONTEXT(target)
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(key: KClass<T>): T = objects[key]!! as T

    operator fun <T : Any> set(key: KClass<T>, value: T) {
        objects[key] = value
    }
}