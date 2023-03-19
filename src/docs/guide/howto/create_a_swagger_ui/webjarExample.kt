package guide.howto.create_a_swagger_ui

import org.http4k.contract.ui.swagger.swaggerUiWebjar
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {
    // Build a Swagger UI for the sample PetStore API
    val ui = swaggerUiWebjar {
        // Provide your own OpenApi spec by overriding the "url" property
        pageTitle = "My PetStore UI"
        displayOperationId = true
    }

    // Combine our api, spec, and ui; then start a server
    // The Swagger UI is available on the root "/" path
    ui
        .asServer(SunHttp(8080))
        .start()
        .block()
}
