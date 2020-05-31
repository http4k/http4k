package org.http4k.serverless

import org.http4k.core.HttpHandler

/**
 * Http4k app loader - instantiate the application from the environment config
 */
interface AppLoader : (Map<String, String>) -> HttpHandler
