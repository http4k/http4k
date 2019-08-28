package org.http4k.contract.openapi

import org.http4k.contract.security.AndSecurity
import org.http4k.contract.security.Security
import org.http4k.format.Json

typealias Render<NODE> = Json<NODE>.() -> NODE

/**
 * Provides rendering of Security models in to OpenApi specs.
 */
interface SecurityRenderer {
    fun <NODE> full(security: Security): Render<NODE>?
    fun <NODE> ref(security: Security): Render<NODE>?

    companion object {
        operator fun invoke(vararg renderers: SecurityRenderer): SecurityRenderer = object : SecurityRenderer {
            override fun <NODE> full(security: Security) =
                renderers.asSequence().mapNotNull { it.full<NODE>(security) }.firstOrNull()

            override fun <NODE> ref(security: Security): Render<NODE>? =
                renderers.asSequence().mapNotNull { it.ref<NODE>(security) }.firstOrNull()
        }
    }
}

interface RenderModes {
    fun <NODE> full(): Render<NODE>
    fun <NODE> ref(): Render<NODE>
}

inline fun <reified T : Security> rendererFor(crossinline fn: (T) -> RenderModes) = object : SecurityRenderer {
    override fun <NODE> full(security: Security): Render<NODE>? = if (security is T) fn(security).full() else null
    override fun <NODE> ref(security: Security): Render<NODE>? = if (security is T) fn(security).ref() else null
}

/**
 * This renderer collects the various renderings from the component parts
 */
fun AndSecurity.Companion.renderer(delegate: SecurityRenderer) = rendererFor<AndSecurity> {
    object : RenderModes {
        override fun <NODE> full(): Render<NODE> = { all(delegate::full) }

        override fun <NODE> ref(): Render<NODE> = { all(delegate::ref) }

        private fun <NODE> Json<NODE>.all(transform: (Security) -> (Json<NODE>.() -> NODE)?): NODE {
            val fields = it
                .mapNotNull(transform)
                .flatMap { fields(it(this)) }
                .toTypedArray()
            return obj(*fields)
        }
    }
}