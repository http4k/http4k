package guide.howto.create_a_swagger_ui

import org.http4k.contract.ui.redoc.redocWebjar
import org.http4k.contract.ui.swagger.swaggerUiWebjar
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {
    val http = routes(
        // bind the API and OpenApi description off of root
        createContractHandler("/openapi.json"),

        // Bind Redoc to the root path
        redocWebjar {
            url = "/openapi.json"
            pageTitle = "Hello Server - WebJar"
            options["disable-search"] = "true"
        },

        // Bind Swagger UI to another path
        "/swagger" bind swaggerUiWebjar {
            url = "/openapi.json"
            pageTitle = "Hello Server"
            displayOperationId = true
        }
    )

    // run the server.  The default UI is available at http://localhost:8080
    http.asServer(SunHttp(8080))
        .start()
        .block()
}


