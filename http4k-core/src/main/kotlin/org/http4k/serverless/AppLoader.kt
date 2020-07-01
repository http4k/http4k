package org.http4k.serverless

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.then

/**
 * Http4k app loader - instantiate the application from the environment config
 */
interface AppLoader : (Map<String, String>) -> HttpHandler

fun Filter.then(appLoader: AppLoader): AppLoader = object : AppLoader {
    override fun invoke(p1: Map<String, String>) = then(appLoader(p1))
}
