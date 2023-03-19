package org.http4k.contract.ui.swagger

import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.routing.ResourceLoader
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import kotlin.io.path.Path

fun swaggerUiWebjar(
    descriptionRoute: Uri,
    title: String = "Swagger UI",
    layout: String = "BaseLayout", // also supports StandardLayout
    deepLinking: Boolean = true,
    displayOperationId: Boolean = false,
    displayRequestDuration: Boolean = false,
    requestSnippetsEnabled: Boolean = false,
    persistAuthorization: Boolean = false,
    queryConfigEnabled: Boolean = false,
    tryItOutEnabled: Boolean = true,
    webjarPath: String = "/webjars/swagger-ui/4.18.1" // override to use your own bundled distribution
): RoutingHttpHandler {
    val initJs = """window.onload = function() {
    document.title = "$title";
    SwaggerUIBundle({
        url: "$descriptionRoute",
        dom_id: '#swagger-ui',
        deepLinking: $deepLinking,
        displayOperationId: $displayOperationId,
        displayRequestDuration: $displayRequestDuration,
        requestSnippetsEnabled: $requestSnippetsEnabled,
        persistAuthorization: $persistAuthorization,
        presets: [
            SwaggerUIBundle.presets.apis,
            SwaggerUIStandalonePreset
        ],
        layout: "$layout",
        queryConfigEnabled: $queryConfigEnabled,
        tryItOutEnabled: $tryItOutEnabled
    })
}
"""
    return routes(
        "swagger-initializer.js" bind Method.GET to {
            Response(Status.OK).body(initJs)
        },
        static(ResourceLoader.Classpath( "/META-INF/resources/" + webjarPath.trimStart('/')))
    )
}
