package org.http4k.connect.openai.plugins

import org.http4k.connect.openai.auth.OpenAIPluginId
import org.http4k.connect.openai.plugins.internal.ForwardCallsToPluginServer
import org.http4k.connect.openai.plugins.internal.LoadOpenApi
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.BearerAuth
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.InsecureCookieBasedOAuthPersistence
import org.http4k.security.OAuthProvider
import org.http4k.security.OAuthProviderConfig
import java.time.Clock
import java.time.Duration

/**
 * Plugin implementation which plugs into the FakeOpenAI server. It performs the
 * oauth flow to the plugin, obtaining a token and driving the login.
 */
fun OAuthPluginIntegration(
    pluginId: OpenAIPluginId,
    pluginOAuthConfig: OAuthProviderConfig,
) = object : PluginIntegration {

    override val pluginId = pluginId

    override fun buildIntegration(openAiUrl: Uri, http: HttpHandler, clock: Clock): IntegratedPlugin {
        val oAuthPersistence = InsecureCookieBasedOAuthPersistence(
            pluginOAuthConfig.apiBase.toString(), Duration.ofSeconds(60), clock
        )

        val oAuthProvider = OAuthProvider(
            pluginOAuthConfig,
            http,
            openAiUrl.path("/aip/plugin-${pluginId}/oauth/callback"),
            emptyList(),
            oAuthPersistence
        )

        return IntegratedPlugin(
            pluginId,
            routes(
                "/aip/plugin-${pluginId}/oauth/callback" bind GET to oAuthProvider.callback,
                LoadOpenApi(pluginId, openAiUrl, http, oAuthProvider.providerConfig.apiBase),
                ForwardCallsToPluginServer(pluginId, http, oAuthProvider.providerConfig.apiBase) {
                    oAuthProvider.authFilter.then(
                        BearerAuth(oAuthPersistence.retrieveToken(it)!!.value)
                    )
                }
            ),
            oAuthProvider.authFilter
        )
    }

}

