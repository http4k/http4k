package guide.howto.create_a_swagger_ui

import org.http4k.contract.ui.redocLite
import org.http4k.contract.ui.swaggerUiLite
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {
    val http = routes(
        // bind the API and OpenApi description off of root
        createContractHandler("/openapi.json"),

        // bind Swagger UI to the root path
        swaggerUiLite {
            url = "/openapi.json"
            pageTitle = "Hello Server - Swagger UI"
            persistAuthorization = true
        },

        // Bind Redoc to another path
        "/redoc" bind redocLite {
            url = "/openapi.json"
            pageTitle = "Hello Server - Redoc"
            options["disable-search"] = "true"
        }
    )

    // run the server.  The default UI is available at http://localhost:8080
    http.asServer(SunHttp(8080))
        .start()
        .block()
}
