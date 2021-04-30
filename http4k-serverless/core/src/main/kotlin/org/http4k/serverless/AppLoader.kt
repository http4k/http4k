package org.http4k.serverless

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.then

/**
 * Http4k app loader - instantiate the application from the environment config
 */
fun interface AppLoader : (Map<String, String>) -> HttpHandler

fun Filter.then(appLoader: AppLoader): AppLoader = AppLoader { then(appLoader(it)) }
