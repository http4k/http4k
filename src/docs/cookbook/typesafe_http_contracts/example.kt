package cookbook.typesafe_http_contracts

import org.http4k.contract.ApiInfo
import org.http4k.contract.ApiKey
import org.http4k.contract.OpenApi
import org.http4k.contract.bind
import org.http4k.contract.bindContract
import org.http4k.contract.contract
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.CachingFilters.Response.NoCache
import org.http4k.filter.CorsPolicy
import org.http4k.filter.ResponseFilters
import org.http4k.filter.ServerFilters
import org.http4k.format.Argo
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import org.http4k.server.Jetty
import org.http4k.server.asServer
import java.time.Clock
import java.time.Duration

fun main(args: Array<String>) {

    fun add(value1: Int, value2: Int): HttpHandler = {
        Response(OK).with(
            Body.string(TEXT_PLAIN).toLens() of (value1 + value2).toString()
        )
    }

    val ageQuery = Query.int().required("age")
    fun echo(name: String): HttpHandler = {
        Response(OK).with(
            Body.string(TEXT_PLAIN).toLens() of "hello $name you are ${ageQuery(it)}"
        )
    }

    val filter: Filter = ResponseFilters.ReportRouteLatency(Clock.systemUTC(), { name: String, latency: Duration ->
        println(name + " took " + latency)
    })

    val security = ApiKey(Query.int().required("apiKey"), {
        println("foo" + (it == 42))
        it == 42
    })

    val contract = contract(OpenApi(ApiInfo("my great api", "v1.0"), Argo), "/docs/swagger.json", security,
        "/ping" meta {
            summary = "add"
            description = "Adds 2 numbers together"
            returning("The result" to OK)
        } bindContract GET to { Response(OK).body("pong") },
        "/add" / Path.int().of("value1") / Path.int().of("value2") meta {
            summary = "add"
            description = "Adds 2 numbers together"
            returning("The result" to OK)
        } bindContract GET
            to ::add,
        "/echo" / Path.of("name") meta {
            summary = "echo"
            queries += ageQuery
        } bindContract GET to ::echo
    )

    val handler = routes(
        "/context" bind filter.then(contract),
        "/static" bind NoCache().then(static(Classpath("cookbook"))),
        "/" bind contract(OpenApi(ApiInfo("my great super api", "v1.0"), Argo),
            "/echo" / Path.of("name") meta {
                summary = "echo"
                queries += ageQuery
            } bindContract GET to ::echo
        )
    )

    ServerFilters.Cors(CorsPolicy.UnsafeGlobalPermissive).then(handler).asServer(Jetty(8000)).start()
}

// Ping!                    curl -v "http://localhost:8000/context/ping?apiKey=42"
// Adding 2 numbers:        curl -v "http://localhost:8000/context/add/123/564?apiKey=42"
// Echo (fail):             curl -v "http://localhost:8000/context/echo/myName?age=notANumber&apiKey=42"
// API Key enforcement:     curl -v "http://localhost:8000/context/add/123/564?apiKey=444"
// Static content:          curl -v "http://localhost:8000/static/someStaticFile.txt"
// OpenApi/Swagger documentation:   curl -v "http://localhost:8000/context/docs/swagger.json"
// Echo endpoint (at root): curl -v "http://localhost:8000/echo/hello?age=123"