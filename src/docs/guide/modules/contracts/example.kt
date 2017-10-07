package guide.modules.contracts

import org.http4k.contract.ApiInfo
import org.http4k.contract.ApiKey
import org.http4k.contract.OpenApi
import org.http4k.contract.RouteMeta
import org.http4k.contract.bind
import org.http4k.contract.bindContract
import org.http4k.contract.body
import org.http4k.contract.contract
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.contract.query
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Argo
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.routing.routes

//1. Define a route
//Firstly, create a route with the desired contract of path, headers, queries and body parameters.
val ageQuery = Query.int().required("age")
val stringBody = Body.string(TEXT_PLAIN).toLens()

val route = ("/echo" / Path.of("name")
    query ageQuery
    body stringBody
    bindContract Method.GET)

//2. Dynamic binding of calls to an HttpHandler
//Next, bind this route to a function which creates an `HttpHandler` for each invocation, which receives the dynamic path elements from the path:
fun echo(nameFromPath: String): HttpHandler = { request: Request ->
    val age = ageQuery(request)
    val sentMessage = stringBody(request)

    Response(OK).with(
        stringBody of "hello $nameFromPath you are $age. You sent $sentMessage"
    )
}

//3. Combining Routes into a contract and bind to a context
//Finally, the `ContractRoutes` are added into a reusable `Contract` in the standard way, defining a renderer (in this example OpenApi/Swagger) and a security model (in this case an API-Key):

val routeWithBindings = route to ::echo meta RouteMeta("echo")

val security = ApiKey(Query.int().required("api"), { it == 42 })

val handler: HttpHandler = routes(
    "/api/v1" bind contract(OpenApi(ApiInfo("My great API", "v1.0"), Argo), "", security,
        routeWithBindings
    )
)
