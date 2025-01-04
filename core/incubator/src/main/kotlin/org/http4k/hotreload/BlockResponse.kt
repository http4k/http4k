package org.http4k.hotreload

import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.Status.Companion.OK
import java.time.Duration

/**
 * Filter which blocks requests to a given path for a specified duration.
 */
fun BlockResponse(path: String, sleeper: (Duration) -> Unit = Thread::sleep) = Filter { next ->
    {
        if (it.uri.path == path) {
            runCatching { sleeper(Duration.ofHours(1)) }
                .map { Response(OK) }
                .getOrDefault(Response(I_M_A_TEAPOT))
        } else next(it)
    }
}
