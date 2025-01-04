package org.http4k.testing

import org.http4k.server.PolyHandler

/**
 * Implement this to provide a PolyHandler for hot-reloading.
 */
fun interface PolyAppProvider : () -> PolyHandler
