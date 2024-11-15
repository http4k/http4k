package org.http4k.connect.openai.auth.service

import org.http4k.connect.openai.auth.PluginAuth
import org.http4k.connect.openai.auth.PluginAuthToken
import org.http4k.connect.openai.model.AuthedSystem
import org.http4k.connect.openai.model.VerificationToken
import org.http4k.routing.RoutingHttpHandler

/**
 * Service plugin auth. The plugin API is protected by credentials set by the plugin owner into OpenAI.
 * This means that there is no possible response personalisation available to the plugin.
 */
class ServiceLevelAuth(
    pluginToken: PluginAuthToken,
    tokens: Map<AuthedSystem, VerificationToken>
) : PluginAuth {
    override val manifestDescription = mapOf(
        "type" to "service_http",
        "authorization_type" to pluginToken.type,
        "verification_tokens" to tokens
    )

    override val securityFilter = pluginToken.securityFilter
    override val authRoutes = emptyList<RoutingHttpHandler>()
}
