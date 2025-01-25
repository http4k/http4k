package org.http4k.connect.openai.auth.noauth

import org.http4k.connect.openai.auth.PluginAuth
import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.routing.RoutingHttpHandler

/**
 * No plugin auth. The plugin API is unprotected.
 */
object NoAuth : PluginAuth {
    override val manifestDescription = mapOf("type" to "none")
    override val securityFilter = Filter.NoOp
    override val authRoutes = emptyList<RoutingHttpHandler>()
}
