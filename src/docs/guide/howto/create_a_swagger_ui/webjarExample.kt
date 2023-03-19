package guide.howto.create_a_swagger_ui

import org.http4k.contract.contract
import org.http4k.contract.ui.swagger.swaggerUiWebjar
import org.http4k.core.Uri
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {
    // Define a contract, and render an OpenApi 3 spec at "/spec"
    val api = contract {
        // add your ContractRoutes as normal
        descriptionPath = "spec"
    }

    // Build a Swagger UI based on the OpenApi spec defined at "/spec"
    val ui = swaggerUiWebjar(
        descriptionRoute = Uri.of("spec"),
        title = "Hello Server",
        displayOperationId = true
    )

    // Combine our api, spec, and ui; then start a server
    // The Swagger UI is available on the root "/" path
    routes(api, ui)
        .asServer(SunHttp(8080))
        .start()
        .block()
}
