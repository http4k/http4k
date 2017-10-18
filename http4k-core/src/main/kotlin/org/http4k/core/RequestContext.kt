package org.http4k.core

import org.http4k.lens.Header
import org.http4k.lens.LensExtractor
import org.http4k.lens.LensInjector
import java.util.*

class RequestContext internal constructor(internal val id: UUID) {
    private val objects = mutableMapOf<String, Any>()

    companion object : LensExtractor<Request, UUID>, LensInjector<UUID, Request> {

        private val X_HTTP4K_CONTEXT = Header.map(UUID::fromString, UUID::toString).required("x-http4k-context")

        override fun <R : Request> invoke(value: UUID, target: R): R = X_HTTP4K_CONTEXT(value, target)

        override fun invoke(target: Request): UUID = X_HTTP4K_CONTEXT(target)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(key: String): T {
        if(!objects.containsKey(key)) throw IllegalStateException("No RequestContext property initialised for key $key")
        return objects[key]!! as T
    }

    fun set(key: String, value: Any) {
        objects[key] = value
    }
}