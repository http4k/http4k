package worked_example._3_adding_another_endpoint

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.routing.by
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer

fun MyMathServer(port: Int): Http4kServer = MyMathsApp().asServer(Jetty(port))

fun MyMathsApp(): HttpHandler = CatchLensFailure.then(
    routes(
        "/ping" to GET by { _: Request -> Response(OK) },
        "/add" to GET by calculate { it.sum() },
        "/multiply" to GET by calculate { it.fold(1) { memo, next -> memo * next } }
    )
)

private fun calculate(fn: (List<Int>) -> Int): (Request) -> Response {
    val values = Query.int().multi.defaulted("value", listOf())

    return {
        request: Request ->
        val valuesToCalc = values.extract(request)
        val answer = if (valuesToCalc.isEmpty()) 0 else fn(valuesToCalc)
        Response(OK).body(answer.toString())
    }
}
