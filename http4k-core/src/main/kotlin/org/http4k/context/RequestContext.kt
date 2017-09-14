package org.http4k.context

import org.http4k.core.Request
import org.http4k.lens.Header
import org.http4k.lens.LensExtractor
import org.http4k.lens.LensInjector
import java.util.*

class RequestContext internal constructor(val id: UUID) {
    private val objects = mutableMapOf<RequestContextKey, Any>()

    companion object : LensExtractor<Request, UUID>, LensInjector<Request, UUID> {

        private val X_HTTP4K_CONTEXT = Header.map(UUID::fromString, UUID::toString).required("x-http4k-context")

        override fun <R : Request> invoke(value: UUID, target: R): R = X_HTTP4K_CONTEXT(value, target)

        override fun invoke(target: Request): UUID = X_HTTP4K_CONTEXT(target)
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(key: RequestContextKey): T = objects[key]!! as T

    operator fun <T : Any> set(key: RequestContextKey, value: T) {
        objects[key] = value
    }
}