package org.http4k.connect.openai.auth.user

import org.http4k.connect.openai.auth.PluginAuth
import org.http4k.connect.openai.auth.PluginAuthToken
import org.http4k.routing.RoutingHttpHandler

/**
 * User plugin auth. The plugin API is protected by a set of user credentials entered into OpenAI.
 * This means that the user principal is known to the plugin and responses can be personalised.
 */
class UserLevelAuth(pluginToken: PluginAuthToken) : PluginAuth {
    override val manifestDescription = mapOf(
        "type" to "user_http",
        "authorization_type" to pluginToken.type
    )

    override val securityFilter = pluginToken.securityFilter
    override val authRoutes = emptyList<RoutingHttpHandler>()
}
