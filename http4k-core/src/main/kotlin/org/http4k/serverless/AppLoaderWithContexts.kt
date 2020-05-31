package org.http4k.serverless

import org.http4k.core.HttpHandler
import org.http4k.core.RequestContexts

/**
 * Http4k app loader - instantiate the application from the environment config and request contexts object
 */
interface AppLoaderWithContexts : (Map<String, String>, RequestContexts) -> HttpHandler
