package org.http4k.connect.openai.auth

import org.http4k.core.Filter
import org.http4k.routing.RoutingHttpHandler

interface PluginAuth {
    val manifestDescription: Map<String, Any>
    val securityFilter: Filter
    val authRoutes: List<RoutingHttpHandler>
}

