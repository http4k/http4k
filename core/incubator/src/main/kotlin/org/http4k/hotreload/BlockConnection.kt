package org.http4k.hotreload

import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status
import java.time.Duration

/**
 * Filter which blocks requests to a given path for a specified duration.
 */
fun BlockConnection(path: String) = Filter { next ->
    {
        if (it.uri.path == path) {
            Thread.sleep(Duration.ofHours(1))
            Response(Status.OK)
        } else next(it)
    }
}
