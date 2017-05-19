package cookbook

import org.http4k.contract.ApiKey
import org.http4k.contract.Root
import org.http4k.contract.Route
import org.http4k.contract.RouteModule
import org.http4k.contract.SimpleJson
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.filter.ReportRouteLatency
import org.http4k.filter.ResponseFilters
import org.http4k.format.Argo
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.server.Jetty
import org.http4k.server.startServer
import java.time.Clock

/**
 * This contract example shows:
 * - 2 endpoints with typesafe contracts (marshalling of path parameters and Body's)
 * - Custom filters (latency)
 * - API key security via a typesafe Query parameter (this can be a header or a body parameter as well)
 * - simple API documentation
 */
fun main(args: Array<String>) {

    fun add(value1: Int, value2: Int): HttpHandler = {
        Response(OK).with(
            Body.string(TEXT_PLAIN) to (value1 + value2).toString()
        )
    }

    fun echo(name: String, age: Int): HttpHandler = {
        Response(OK).with(
            Body.string(TEXT_PLAIN) to "hello $name you are $age"
        )
    }

    val handler = RouteModule(Root / "context", SimpleJson(Argo), ResponseFilters.ReportRouteLatency(Clock.systemUTC(), {
        name, latency ->
        println(name + " took " + latency)
    }))
        .securedBy(ApiKey(Query.int().required("apiKey"), { it == 42 }))
        .withRoute(Route("add").at(GET) / "add" / Path.int().of("value1") / Path.int().of("value2") bind ::add)
        .withRoute(Route("echo").at(GET) / "echo" / Path.of("name") / Path.int().of("age") bind ::echo)
        .toHttpHandler()

    handler.startServer(Jetty(8000))
}

// Adding 2 numbers:      curl -v "http://localhost:8000/context/add/123/564?apiKey=42"
// API Key enforcement:   curl -v "http://localhost:8000/context/add/123/564?apiKey=444"
// Auto-documentation:    curl -v "http://localhost:8000/context?apiKey=42"
