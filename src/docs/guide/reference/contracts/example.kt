package guide.reference.contracts

// for this example we're using Jackson - note that the auto method imported is an extension
// function that is defined on the Jackson instance

import org.http4k.contract.ContractRoute
import org.http4k.contract.Tag
import org.http4k.contract.bind
import org.http4k.contract.bindCallback
import org.http4k.contract.contract
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.contract.security.ApiKeySecurity
import org.http4k.contract.security.BasicAuthSecurity
import org.http4k.contract.ui.swaggerUiLite
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.format.Jackson.auto
import org.http4k.format.Klaxon.json
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer

// 1. this route has no dynamic path segments and simply echoes the body back. We are also adding various metadata
// to the route, which will be used in the OpenAPI documentation. All of the metadata is optional.
fun echo(): ContractRoute =
    "/echo" meta {
        summary = "echoes the body back"
        description = "This is a simple route which echoes the body back to the caller"
        tags += Tag("env", "production")
        consumes += TEXT_PLAIN
        produces += TEXT_PLAIN
        returning(OK, I_M_A_TEAPOT)
    } bindContract POST to { req: Request ->
        Response(OK).body(req.body)
    }

// 2. this route has a dynamic path which is automatically injected into the handler and security applied. There
// are various security instances, from Basic to APIKey to OAuth2
fun securelyGreet(): ContractRoute =
    "/greet" / Path.of("name") meta {
        security = BasicAuthSecurity("myrealm", Credentials("user", "password"))
    } bindContract POST to { name ->
        { _: Request ->
            Response(OK).body("hello $name")
        }
    }

// 3. this route uses a query lens to extract a parameter from the query string. we add the lens to the contract metadata
fun copy(): ContractRoute {
    val times = Query.int().required("times")

    return "/copy" meta {
        // register the query lens with the contract
        queries += times
    } bindContract POST to
        { req: Request ->
            // extract the value from the request using the lens
            val copies: Int = times(req)
            Response(OK).body(req.bodyString().repeat(copies))
        }
}

// 4. echoing JSON
fun echoJson(): ContractRoute {
    data class NameAndMessage(val name: String, val message: String)

    // the body lens here is imported as an extension function from the Jackson instance
    val body = Body.auto<NameAndMessage>().toLens()

    return "/echoJson" meta {
        // register the receiving and returning lenses - these also set the content type
        receiving(body)
        returning(OK, body to NameAndMessage("jim", "hello!"))
    } bindContract POST to { req: Request ->
        val input: NameAndMessage = body(req)

        // we can inject the type directly into the response using either...
        Response(OK).with(body of input)

        // ... or the more convenient... (note that json() is an extension function from the Jackson instance)
        Response(OK).json(input)
    }
}

// this route has a callback registered, so can be used when processes have asynchronous updates
// they will be POSTed back to callbackUrl received in the request
fun routeWithCallback(): ContractRoute {

    data class StartProcess(val callbackUrl: Uri)

    val body = Body.auto<StartProcess>().toLens()

    val spec = "/callback" meta {
        summary = "kick off a process with an async callback"

        // register the callback for later updates. The syntax of the callback URL comes
        // from the OpenApi spec
        callback("update") {
            """{${"$"}request.body#/callbackUrl}""" meta {
                receiving(body to StartProcess(Uri.of("http://caller")))
            } bindCallback POST
        }
    } bindContract POST

    val echo: HttpHandler = { request: Request ->
        println(body(request))
        Response(OK)
    }

    return spec to echo
}

// Combine the Routes into a contract and bind to a context, defining a renderer (in this example
// OpenApi/Swagger) and a global security model (in this case an API-Key):
val contract = contract {
    renderer = OpenApi3(ApiInfo("My great API", "v1.0"), Jackson)
    descriptionPath = "/openapi.json"
    security = ApiKeySecurity(Query.required("api_key"), { it.isNotEmpty() })

    routes += echo()
    routes += echoJson()
    routes += copy()
    routes += securelyGreet()
    routes += routeWithCallback()
}

val handler: HttpHandler = routes("/api/v1" bind contract)

// by default, the OpenAPI docs live at the root of the contract context, but we can override it..
fun main() {
    println(handler(Request(GET, "https://localhost:10000/api/v1/openapi.json")))
}
