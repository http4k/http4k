package org.http4k.contract.ui

import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.routing.ResourceLoader
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.static

fun swaggerUi(
    descriptionRoute: Uri,
    title: String = "Swagger UI",
    displayOperationId: Boolean = false,
    displayRequestDuration: Boolean = false,
    requestSnippetsEnabled: Boolean = false,
    persistAuthorization: Boolean = false,
): RoutingHttpHandler = Filter { next ->
    { req ->
        next(req).let { resp ->
            resp.body(
                resp.bodyString()
                    .replace("%%DESCRIPTION_ROUTE%%", descriptionRoute.toString())
                    .replace("%%PAGE_TITLE%%", title)
                    .replace("%%DISPLAY_OPERATION_ID%%", displayOperationId.toString())
                    .replace("%%DISPLAY_REQUEST_DURATION%%", displayRequestDuration.toString())
                    .replace("%%REQUEST_SNIPPETS_ENABLED%%", requestSnippetsEnabled.toString())
                    .replace("%%PERSIST_AUTHORIZATION%%", persistAuthorization.toString())
            ).with(Header.CONTENT_TYPE of ContentType.TEXT_HTML)
        }
    }
}.then(
    static(ResourceLoader.Classpath("org/http4k/contract/ui/public"))
)
