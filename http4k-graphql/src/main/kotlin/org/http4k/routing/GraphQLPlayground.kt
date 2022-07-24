package org.http4k.routing

import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.routing.ResourceLoader.Companion.Classpath

/**
 * Serves the GraphQL playground content.
 */
fun graphQLPlayground(graphQLRoute: Uri, title: String = "GraphQL Playground"): HttpHandler = {
    static(Classpath("org/http4k/routing/public"))(Request(GET, "index.html")).let {
        it.body(
            it.bodyString()
                .replace("%%TITLE%%", title)
                .replace("%%GRAPHQL_ROUTE%%", graphQLRoute.toString())
        ).with(CONTENT_TYPE of TEXT_HTML)
    }
}
