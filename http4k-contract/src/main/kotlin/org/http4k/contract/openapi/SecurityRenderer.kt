package org.http4k.contract.openapi

import org.http4k.contract.security.AndSecurity
import org.http4k.contract.security.OrSecurity
import org.http4k.contract.security.Security

/**
 * Provides rendering of Security models in to OpenApi specs.
 */
interface SecurityRenderer {
    fun <NODE> full(security: Security): Render<NODE>?
    fun <NODE> ref(security: Security): Render<NODE>?

    companion object {
        operator fun invoke(vararg renderers: SecurityRenderer): SecurityRenderer = object : SecurityRenderer {
            override fun <NODE> full(security: Security): Render<NODE>? = when (security) {
                is AndSecurity -> security.renderAll { full<NODE>(it) }?.toObj()
                is OrSecurity -> security.renderAll { full<NODE>(it) }?.toObj()
                else -> renderers.asSequence().mapNotNull { it.full<NODE>(security) }.firstOrNull()
            }

            override fun <NODE> ref(security: Security): Render<NODE>? = when (security) {
                is AndSecurity -> security.renderAll { ref<NODE>(it) }?.toObj()
                is OrSecurity -> security.renderAll { ref<NODE>(it) }?.toArray()
                else -> renderers.asSequence().mapNotNull { it.ref<NODE>(security) }.firstOrNull()
            }

            private fun <NODE> Iterable<Security>.renderAll(transform: (Security) -> Render<NODE>?) =
                mapNotNull(transform).takeIf { it.isNotEmpty() }

            private fun <NODE> List<Render<NODE>>.toObj(): Render<NODE> = {
                obj(flatMap { fields(it(this)) })
            }

            private fun <NODE> List<Render<NODE>>.toArray(): Render<NODE> = {
                array(flatMap { fields(it(this)) }.map { obj(it) })
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
