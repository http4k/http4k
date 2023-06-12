package guide.howto.integrate_with_openapi

import org.http4k.contract.bind
import org.http4k.contract.contract
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.ApiServer
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.contract.security.ApiKeySecurity
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.CachingFilters.Response.NoCache
import org.http4k.filter.CorsPolicy
import org.http4k.filter.ResponseFilters
import org.http4k.filter.ServerFilters
import org.http4k.format.Argo
import org.http4k.format.Jackson
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

fun main() {

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

    val filter: Filter =
        ResponseFilters.ReportHttpTransaction(Clock.systemUTC()) { tx: HttpTransaction ->
            println(tx.labels.toString() + " took " + tx.duration)
        }

    val mySecurity = ApiKeySecurity(Query.int().required("apiKey"), { it == 42 })

    val contract = contract {
        renderer = OpenApi3(
            ApiInfo("my great api", "v1.0"),
            Argo,
            servers = listOf(ApiServer(Uri.of("http://localhost:8000"), "the greatest server"))
        )
        descriptionPath = "/docs/openapi.json"
        security = mySecurity

        routes += "/ping" meta {
            summary = "add"
            description = "Adds 2 numbers together"
            returning(OK to "The result")
        } bindContract GET to { _ -> Response(OK).body("pong") }

        routes += "/add" / Path.int().of("value1") / Path.int().of("value2") meta {
            summary = "add"
            description = "Adds 2 numbers together"
            returning(OK to "The result")
        } bindContract GET to ::add

        // note here that the trailing parameter can be ignored - it would simply be the value "divide".
        routes += Path.int().of("value1") / Path.int().of("value2") / "divide" meta {
            summary = "divide"
            description = "Divides 2 numbers"
            returning(OK to "The result")
        } bindContract GET to { first, second, _ ->
            { Response(OK).body((first / second).toString()) }
        }

        routes += "/echo" / Path.of("name") meta {
            summary = "echo"
            queries += ageQuery
        } bindContract GET to ::echo
    }

    val handler = routes(
        "/context" bind filter.then(contract),
        "/static" bind NoCache().then(static(Classpath("guide/howto/nestable_routes"))),
        "/" bind contract {
            renderer = OpenApi3(ApiInfo("my great super api", "v1.0"), Jackson)
            routes += "/echo" / Path.of("name") meta {
                summary = "echo"
                queries += ageQuery
            } bindContract GET to ::echo
        }
    )

    ServerFilters.Cors(CorsPolicy.UnsafeGlobalPermissive).then(handler).asServer(Jetty(8000))
        .start()
}

// Ping!                    curl -v "http://localhost:8000/context/ping?apiKey=42"
// Adding 2 numbers:        curl -v "http://localhost:8000/context/add/123/564?apiKey=42"
// Echo (fail):             curl -v "http://localhost:8000/context/echo/myName?age=notANumber&apiKey=42"
// API Key enforcement:     curl -v "http://localhost:8000/context/add/123/564?apiKey=444"
// Static content:          curl -v "http://localhost:8000/static/someStaticFile.txt"
// OpenApi/Swagger documentation:   curl -v "http://localhost:8000/context/docs/openapi.json"
// Echo endpoint (at root): curl -v "http://localhost:8000/echo/hello?age=123"
