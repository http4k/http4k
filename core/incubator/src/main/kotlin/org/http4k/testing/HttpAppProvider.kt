package org.http4k.testing

import org.http4k.core.HttpHandler

/**
 * Implement this to provide an HttpHandler for hot-reloading.
 */
fun interface HttpAppProvider : () -> HttpHandler
