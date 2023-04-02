package guide.howto.create_a_swagger_ui

import org.http4k.contract.contract
import org.http4k.contract.meta
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.contract.ui.redoc.redocWebjar
import org.http4k.contract.ui.redocLite
import org.http4k.contract.ui.swagger.swaggerUiWebjar
import org.http4k.contract.ui.swaggerUiLite
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {
    val greetingLens = Body.string(ContentType.TEXT_PLAIN).toLens()

    // Define a single http route for our contract
    val helloHandler = "/v1/hello" meta {
        operationId = "v1Hello"
        summary = "Say Hello"
        returning(OK, greetingLens to "Sample Greeting")
    } bindContract GET to { _: Request ->
        Response(OK).with(greetingLens of "HI!")
    }

    // Define a contract, and render an OpenApi 3 spec at "/openapi"
    val v1Api = contract {
        routes += helloHandler
        renderer = OpenApi3(
            ApiInfo("Hello Server - Developer UI", "99.3.4")
        )
        descriptionPath = "openapi"
    }

    // Build a Swagger UI and point it at the OpenApi spec we defined earlier
    val defaultUi = swaggerUiLite {
        url = "/openapi"
        pageTitle = "Hello Server"
        persistAuthorization = true
    }

    // bind the API and several UIs
    val http = routes(
        v1Api,
        defaultUi,
        "/swagger-lite" bind defaultUi,
        "/redoc-lite" bind redocLite {
            url = "/openapi"
            pageTitle = "Hello Server - Lite"
            options["minCharacterLengthToInitSearch"] = "3"
        },
        "/swagger-webjar" bind swaggerUiWebjar {
            url = "/openapi"
            pageTitle = "Hello Server"
            displayOperationId = true
        },
        "/redoc-webjar" bind redocWebjar {
            url = "/openapi"
            pageTitle = "Hello Server - WebJar"
            options["disable-search"] = "true"
        }
    )

    // run the server.  The default Swagger UI is available at http://localhost:8080
    http.asServer(SunHttp(8080))
        .start()
        .block()
}
