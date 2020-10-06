package org.http4k.core

import org.http4k.lens.Header
import org.http4k.lens.LensExtractor
import org.http4k.lens.LensInjector
import java.util.UUID

class RequestContext internal constructor(val id: UUID = UUID.randomUUID()) {
    private val objects = mutableMapOf<String, Any>()

    companion object : LensExtractor<Request, UUID>, LensInjector<UUID, Request> {

        private val X_HTTP4K_CONTEXT = Header.map(UUID::fromString, UUID::toString).required("x-http4k-context")

        override fun <R : Request> invoke(value: UUID, target: R): R = X_HTTP4K_CONTEXT(value, target)

        override fun invoke(target: Request): UUID = X_HTTP4K_CONTEXT(target)
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any?> get(key: String): T? = objects[key] as T?

    operator fun set(key: String, value: Any?) {
        value?.let { objects[key] = value } ?: objects.remove(key)
    }
}
