package org.http4k.connect.openai.endpoints

import org.http4k.connect.openai.auth.PluginAuth
import org.http4k.core.Filter
import org.http4k.core.then

internal fun ProtectPluginRoutes(auth: PluginAuth) = Filter { next ->
    {
        when (it.uri.path) {
            "/openapi.json" -> next(it)
            "/.well-known/ai-plugin.json" -> next(it)
            else -> auth.securityFilter.then(next)(it)
        }
    }
}
