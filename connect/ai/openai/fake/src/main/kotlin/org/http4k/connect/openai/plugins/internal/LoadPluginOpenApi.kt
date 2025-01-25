package org.http4k.connect.openai.plugins.internal

import org.http4k.connect.openai.auth.OpenAIPluginId
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.routing.bind

internal fun LoadOpenApi(pluginId: OpenAIPluginId, openAiUrl: Uri, http: HttpHandler, baseUri: Uri) =
    "/${pluginId}/openapi.json" bind GET to {
        SetHostFrom(baseUri).then(http)(it.removePluginId(pluginId))
            .let {
                it.body(
                    it.bodyString().replace(
                        baseUri.toString(),
                        "$openAiUrl/$pluginId/"
                    )
                )
            }
    }
