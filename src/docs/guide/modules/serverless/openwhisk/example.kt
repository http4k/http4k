package guide.modules.serverless.openwhisk

import com.google.gson.JsonObject
import org.http4k.client.ApacheClient
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.format.Gson
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.serverless.AppLoader
import org.http4k.serverless.FakeOpenWhiskRequest
import org.http4k.serverless.OpenWhiskFunction

// This AppLoader is responsible for building our HttpHandler which is supplied to OpenWhisk
// Along with the extension class below, is the only actual piece of code that needs to be written.
object TweetEchoLambda : AppLoader {
    private val timer = Filter { next: HttpHandler ->
        { request: Request ->
            val start = System.currentTimeMillis()
            val response = next(request)
            val latency = System.currentTimeMillis() - start
            println("I took $latency ms")
            response
        }
    }

    override fun invoke(env: Map<String, String>): HttpHandler =
        timer
            .then(
                routes(
                    "/echo" bind POST to { Response(OK).body(it.bodyString().take(18)) }
                )
            )
}

// This class is the entry-point for the function call - configure it when deploying
class FunctionsExampleEntryClass : OpenWhiskFunction(TweetEchoLambda)

fun main() {

    // Launching your Function locally - by simply providing the operating ENVIRONMENT map as would
    // be configured in OpenWhisk.
    fun runFunctionLocally() {
        println("RUNNING LOCALLY:")

        val app: HttpHandler = TweetEchoLambda(System.getenv())
        val localLambda = app.asServer(SunHttp(8000)).start()

        println(ApacheClient()(Request(POST, "http://localhost:8000/echo").body("hello hello hello, i suppose this isn't 140 characters anymore..")))
        localLambda.stop()
    }

    // the following code is purely here for demonstration purposes, to explain exactly what is happening in OpenWhisk.
    fun runFunctionAsOpenWhiskWould() {
        println("RUNNING AS OpenWhisk:")

        val fakeOpenWhiskRequest = FakeOpenWhiskRequest(
            "POST", "/echo", emptyMap(), emptyMap(),
            "hello hello hello, i suppose this isn't 140 characters anymore.."
        )

        val response = FunctionsExampleEntryClass().main(Gson.asJsonObject(fakeOpenWhiskRequest) as JsonObject)
        println(response)
    }

    runFunctionLocally()
    runFunctionAsOpenWhiskWould()
}
