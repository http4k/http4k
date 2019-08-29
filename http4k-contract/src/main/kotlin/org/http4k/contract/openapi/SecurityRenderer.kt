package org.http4k.contract.openapi

import org.http4k.contract.security.AndSecurity
import org.http4k.contract.security.OrSecurity
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
            override fun <NODE> full(security: Security): Render<NODE>? = when (security) {
                is AndSecurity -> security.mapNotNull { full<NODE>(it) }.takeIf { it.isNotEmpty() }?.toObj()
                is OrSecurity -> security.mapNotNull { full<NODE>(it) }.takeIf { it.isNotEmpty() }?.toObj()
                else -> renderers.asSequence().mapNotNull { it.full<NODE>(security) }.firstOrNull()
            }

            override fun <NODE> ref(security: Security): Render<NODE>? = when (security) {
                is AndSecurity -> security.mapNotNull { ref<NODE>(it) }.takeIf { it.isNotEmpty() }?.toObj()
                is OrSecurity -> security.mapNotNull { ref<NODE>(it) }.takeIf { it.isNotEmpty() }?.toObj()
                else -> renderers.asSequence().mapNotNull { it.ref<NODE>(security) }.firstOrNull()
            }

            private fun <NODE> List<Render<NODE>>.toObj(): Json<NODE>.() -> NODE = {
                obj(*flatMap { fields(it(this)) }.toTypedArray())
            }
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