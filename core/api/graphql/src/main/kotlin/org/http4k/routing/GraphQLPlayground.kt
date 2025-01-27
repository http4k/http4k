package org.http4k.routing

import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Filter
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.routing.ResourceLoader.Companion.Classpath

/**
 * Serves the GraphQL playground content.
 */
fun graphQLPlayground(graphQLRoute: Uri, title: String = "GraphQL Playground") =
    Filter { next ->
        {
            next(it).let {
                it.body(
                    it.bodyString()
                        .replace("%%TITLE%%", title)
                        .replace("%%GRAPHQL_ROUTE%%", graphQLRoute.toString())
                ).with(CONTENT_TYPE of TEXT_HTML)
            }
        }
    }.then(
        static(Classpath("org/http4k/routing/public"))
    )
