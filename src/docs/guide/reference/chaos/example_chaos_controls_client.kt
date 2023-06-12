package guide.reference.chaos

import org.http4k.chaos.ChaosBehaviours.ReturnStatus
import org.http4k.chaos.ChaosEngine
import org.http4k.chaos.withChaosApi
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes

fun main() {
    val app = routes("/" bind routes("/" bind GET to { Response(OK).body("hello!") }))

    val appWithChaos = app.withChaosApi(ChaosEngine(ReturnStatus(NOT_FOUND)))

    println(">>chaos is deactivated by default")
    println(appWithChaos(Request(GET, "/chaos/status")).bodyString())
    println(appWithChaos(Request(GET, "/")).status)

    println(">>activate the default chaos")
    println(appWithChaos(Request(POST, "/chaos/activate")).bodyString())
    println(appWithChaos(Request(GET, "/")).status)

    println(">>deactivate the default chaos")
    println(appWithChaos(Request(POST, "/chaos/deactivate")).bodyString())
    println(appWithChaos(Request(GET, "/")).status)

    println(">>set the chaos dynamically")
    val alwaysReturn418 = """[
        {
          "type": "trigger",
          "behaviour": {
            "type": "status",
            "status": 418
          },
          "trigger": {
            "type": "always"
          }
        }]
        """.trimIndent()
    println(
        appWithChaos(
            Request(
                POST,
                "/chaos/activate/new"
            ).body(alwaysReturn418)
        ).bodyString()
    )
    println(appWithChaos(Request(GET, "/")).status)
}
