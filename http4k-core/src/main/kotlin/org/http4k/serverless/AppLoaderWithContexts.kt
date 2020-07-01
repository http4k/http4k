package org.http4k.serverless

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.RequestContexts
import org.http4k.core.then

/**
 * Http4k app loader - instantiate the application from the environment config and RequestContexts
 */
interface AppLoaderWithContexts : (Map<String, String>, RequestContexts) -> HttpHandler

fun Filter.then(appLoader: AppLoaderWithContexts) = object : AppLoaderWithContexts {
    override fun invoke(p1: Map<String, String>, contexts: RequestContexts) = then(appLoader(p1, contexts))
}
