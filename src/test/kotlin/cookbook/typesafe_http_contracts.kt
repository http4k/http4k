package cookbook

import org.http4k.contract.Root
import org.http4k.contract.Route
import org.http4k.contract.RouteModule
import org.http4k.contract.SimpleJson
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.filters.ReportRouteLatency
import org.http4k.filters.ResponseFilters
import org.http4k.format.Argo
import org.http4k.lens.Body
import org.http4k.lens.Path
import org.http4k.lens.int
import org.http4k.server.asJettyServer
import java.time.Clock


fun main(args: Array<String>) {

    fun add(value1: Int, value2: Int): HttpHandler = {
        Response(OK).with(
            Body.string(TEXT_PLAIN).required() to (value1 + value2).toString()
        )
    }

    fun echo(name: String, age: Int): HttpHandler = {
        Response(OK).with(
            Body.string(TEXT_PLAIN).required() to "hello $name you are $age"
        )
    }

    val handler = RouteModule(Root / "foo", SimpleJson(Argo), ResponseFilters.ReportRouteLatency(Clock.systemUTC(), {
        name, latency ->
        println(name + " took " + latency)
    }))
//        .securedBy(ApiKey(Query.int().required("api"), { it == 42 }))
        .withRoute(Route("add").at(GET) / "add" / Path.int().of("value1") / Path.int().of("value2") bind ::add)
        .withRoute(Route("echo").at(GET) / "echo" / Path.of("name") / Path.int().of("age") bind ::echo)
        .toHttpHandler()

    handler.asJettyServer(8000).start().block()
}
