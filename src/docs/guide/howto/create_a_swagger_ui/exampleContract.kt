package guide.howto.create_a_swagger_ui

import org.http4k.contract.ContractRoutingHttpHandler
import org.http4k.contract.contract
import org.http4k.contract.meta
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.lens.string

fun createContractHandler(descriptionPath: String): ContractRoutingHttpHandler {
    val greetingLens = Body.string(ContentType.TEXT_PLAIN).toLens()

    // Define a single http route for our contract
    val helloHandler = "/v1/hello" meta {
        operationId = "v1Hello"
        summary = "Say Hello"
        returning(Status.OK, greetingLens to "Sample Greeting")
    } bindContract Method.GET to { _: Request ->
        Response(Status.OK).with(greetingLens of "HI!")
    }

    // Define a contract, and render an OpenApi 3 spec at the given path
    return contract {
        routes += helloHandler
        renderer = OpenApi3(
            ApiInfo("Hello Server - Developer UI", "99.3.4")
        )
        this.descriptionPath = descriptionPath
    }
}
