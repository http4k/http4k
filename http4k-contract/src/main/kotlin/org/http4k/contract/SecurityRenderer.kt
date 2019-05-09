package org.http4k.contract

import org.http4k.format.Json

typealias Render<NODE> = Json<NODE>.() -> NODE

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