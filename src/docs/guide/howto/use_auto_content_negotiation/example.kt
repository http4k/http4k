package guide.howto.use_auto_content_negotiation

import org.http4k.contract.ContractRoute
import org.http4k.contract.contract
import org.http4k.contract.meta
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.JacksonXml
import org.http4k.format.Moshi
import org.http4k.format.MoshiYaml
import org.http4k.format.auto
import org.http4k.lens.ContentNegotiation
import java.util.UUID

// define a Person to be our request/response body
private class Person(val id: UUID, val name: String)

fun main() {
    val jsonLens = Moshi.autoBody<Person>().toLens()
    val xmlLens = JacksonXml.autoBody<Person>().toLens()
    val yamlLens = MoshiYaml.autoBody<Person>().toLens()

    /*
     * Create a content negotiator, which will handle lens selection for request and
     * response bodies.
     * Since the json lens is first, it will be the fallback when no accepted lens is found.
     */
    val negotiator = ContentNegotiation.auto(jsonLens, xmlLens, yamlLens)

    // This sample will be used to populate our contract
    val samplePerson = Person(UUID.fromString("7eb9a4b7-2e50-40d7-b4d2-16ce500a0245"), "john")

    // Create a contract route that will accept a person, and echo it back.
    val echoPerson: ContractRoute = "/echo" meta {
        summary = "Echo Person"
        receiving(negotiator to samplePerson) // add request bodies to contract
        returning(OK, negotiator to samplePerson) // add response bodies to contract
    } bindContract POST to { req: Request ->
        // Unmarshall the body based on the CONTENT-TYPE header
        val person = negotiator(req)

        // select the appropriate outbound lens based on the ACCEPT header
        val outboundLens =
            negotiator.outbound(req)
        Response(OK).with(outboundLens of person)
    }

    // Create an HttpHandler
    val handler: HttpHandler = contract {
        routes += echoPerson
        renderer = OpenApi3(
            ApiInfo("Content Negotiator Sample API", "v1.0")
        )
    }

    // Send a request with a json request body, accepting a YAML response body
    Request(POST, "/echo")
        .with(jsonLens of samplePerson)
        .header("ACCEPT", yamlLens.contentType.toHeaderValue())
        .let(handler)
        .also { println(it.bodyString()) }

    // Send a request with an xml request body, which will return the default json response body
    Request(POST, "/echo")
        .with(xmlLens of samplePerson)
        .let(handler)
        .also { println(it.bodyString()) }
}
