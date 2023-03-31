package guide.howto.create_a_swagger_ui

import org.http4k.contract.contract
import org.http4k.contract.meta
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.contract.openapi.v3.OpenApi3ApiRenderer
import org.http4k.contract.ui.redoc.redocWebjar
import org.http4k.contract.ui.redocLite
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.Argo
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {
    val greetingLens = Body.string(ContentType.TEXT_PLAIN).toLens()

    val api = contract {
        routes += "/v1/hello" meta {
            operationId = "v1Hello"
            summary = "Say Hello"
            returning(Status.OK, greetingLens to "Sample Greeting")
        } bindContract Method.GET to { _: Request ->
            Response(Status.OK).with(greetingLens of "HI!")
        }

        descriptionPath = "openapi"
        renderer = OpenApi3(
            ApiInfo("Hello Server", "1.0"),
            json = Argo,
            apiRenderer = OpenApi3ApiRenderer(Argo)
        )
    }

    val ui = redocLite {
        descriptionUrl = "openapi"
        pageTitle = "Hello Server - Lite"
    }

    val webjarUi = redocWebjar {
        descriptionUrl = "openapi"
        pageTitle = "Hello Server - WebJar"
    }

    routes(
        api,
        ui,
        "/webjar" bind webjarUi
    )
        .asServer(SunHttp(8000))
        .start()
        .block()
}
