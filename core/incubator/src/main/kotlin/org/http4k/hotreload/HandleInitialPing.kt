package org.http4k.hotreload

import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status

/**
 * Handles the initial ping from the hot-reloaded page.
 */
fun HandleInitialPing(pingPath: String) = Filter { next ->
    {
        if (it.uri.path == pingPath) Response(Status.OK).body("pong")
        else next(it)
    }
}
