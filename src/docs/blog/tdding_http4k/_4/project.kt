package blog.tdding_http4k._4

import org.http4k.client.OkHttp
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer

class Recorder(private val client: HttpHandler) {
    fun record(value: Int) {
        val response = client(Request(POST, "/$value"))
        if (response.status != ACCEPTED) throw RuntimeException("recorder returned ${response.status}")
    }
}

fun MyMathsApp(recorderHttp: HttpHandler): HttpHandler {
    val recorder = Recorder(recorderHttp)
    return CatchLensFailure.then(
        routes(
            "/ping" bind GET to { _: Request -> Response(OK) },
            "/add" bind GET to calculate(recorder) { it.sum() },
            "/multiply" bind GET to calculate(recorder) { it.fold(1) { memo, next -> memo * next } }
        )
    )
}

private fun calculate(recorder: Recorder, fn: (List<Int>) -> Int): (Request) -> Response {
    val values = Query.int().multi.defaulted("value", listOf())

    return { request: Request ->
        val valuesToCalc = values(request)
        val answer = if (valuesToCalc.isEmpty()) 0 else fn(valuesToCalc)
        recorder.record(answer)
        Response(OK).body(answer.toString())
    }
}

fun MyMathServer(port: Int, recorderBaseUri: Uri): Http4kServer =
    MyMathsApp(SetHostFrom(recorderBaseUri).then(OkHttp())).asServer(Jetty(port))

