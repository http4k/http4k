package org.http4k.routing

import org.http4k.core.Response
import org.http4k.core.Status.Companion.PERMANENT_REDIRECT
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.Header.LOCATION
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.http4k.routing.Versions.HTMX_VERSION
import org.http4k.routing.Versions.HYPERSCRIPT_VERSION

/**
 * Convenience installation of HTML Webjar
 */
fun htmxWebjars() = routes(
    "/hyperscript.js" bind {
        Response(PERMANENT_REDIRECT)
            .with(LOCATION of Uri.of("/_hyperscript.js"))
    },
    "/hyperscript.min.js" bind {
        Response(PERMANENT_REDIRECT)
            .with(LOCATION of Uri.of("/_hyperscript.min.js"))
    },
    static(Classpath("/META-INF/resources/webjars/htmx.org/$HTMX_VERSION/dist")),
    static(Classpath("/META-INF/resources/webjars/hyperscript.org/$HYPERSCRIPT_VERSION/dist")),
)

@Deprecated("renamed to htmxWebjars()")
fun htmxWebjar() = htmxWebjars()

private object Versions {
    const val HTMX_VERSION = "1.9.6"
    const val HYPERSCRIPT_VERSION = "0.9.11"
}
