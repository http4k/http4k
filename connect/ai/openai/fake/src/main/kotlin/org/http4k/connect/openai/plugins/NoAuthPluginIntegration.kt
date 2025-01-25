package org.http4k.connect.openai.plugins

import org.http4k.connect.openai.auth.OpenAIPluginId
import org.http4k.connect.openai.plugins.internal.ForwardCallsToPluginServer
import org.http4k.connect.openai.plugins.internal.LoadOpenApi
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.NoOp
import org.http4k.core.Uri
import org.http4k.routing.routes
import java.time.Clock

/**
 * Plugin implementation which plugs into the FakeOpenAI server. There is no Auth
 */
fun NoAuthPluginIntegration(
    pluginId: OpenAIPluginId,
    pluginUri: Uri
) = object : PluginIntegration {
    override val pluginId = pluginId

    override fun buildIntegration(openAiUrl: Uri, http: HttpHandler, clock: Clock) =
        IntegratedPlugin(
            pluginId,
            routes(
                LoadOpenApi(pluginId, openAiUrl, http, pluginUri),
                ForwardCallsToPluginServer(pluginId, http, pluginUri) {
                    Filter.NoOp
                }
            ),
            Filter.NoOp
        )
}
