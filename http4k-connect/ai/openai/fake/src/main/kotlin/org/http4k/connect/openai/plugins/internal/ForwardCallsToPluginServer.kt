package org.http4k.connect.openai.plugins.internal

import org.http4k.connect.openai.auth.OpenAIPluginId
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.filter.debug
import org.http4k.routing.asRouter
import org.http4k.routing.bind

internal fun ForwardCallsToPluginServer(
    pluginId: OpenAIPluginId,
    http: HttpHandler,
    baseUri: Uri,
    toFilter: (Request) -> Filter
) = { req: Request -> req.uri.path.startsWith("/${pluginId}") }.asRouter() bind
    {
        val pluginServer = toFilter(it)
            .then(SetHostFrom(baseUri))
            .then(http).debug()


        pluginServer(it.removePluginId(pluginId))
    }
