package guide.howto.create_a_swagger_ui

import org.http4k.contract.contract
import org.http4k.contract.meta
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.contract.security.OpenIdConnectSecurity
import org.http4k.contract.ui.swaggerUiLite
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.string
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

    // Define an OpenId Connect Security scheme
    val openIdConnectSecurity = OpenIdConnectSecurity(
        Uri.of("https://accounts.google.com/.well-known/openid-configuration"),
        Filter.NoOp
    )

    // Define a contract, and render an OpenApi 3 spec at "/spec"
    val v1Api = contract {
        routes += helloHandler
        renderer = OpenApi3(
            ApiInfo("Hello Server - Developer UI", "99.3.4")
        )
        descriptionPath = "spec"
        security = openIdConnectSecurity
    }

    // Build a Swagger UI
    val ui = swaggerUiLite {
        url = "spec" // Point to your own OpenApi Spec; defaults to the sample PetStore API
        pageTitle = "Hello Server"
        displayOperationId = true
    }

    // Combine our api, spec, and ui; then start a server
    // The Swagger UI is will be available at http://localhost:8080
    routes(v1Api, ui)
        .asServer(SunHttp(8080))
        .start()
        .block()
}
