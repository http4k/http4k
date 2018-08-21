package guide.modules.chaos

import org.http4k.chaos.ChaosBehaviours.ReturnStatus
import org.http4k.chaos.ChaosTriggers.Always
import org.http4k.chaos.appliedWhen
import org.http4k.chaos.withChaosControls
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes

fun main(args: Array<String>) {
    val app = routes("/" bind routes("/" bind GET to { Response(Status.OK).body("hello!") }))

    val appWithChaos = app.withChaosControls(ReturnStatus(Status.NOT_FOUND).appliedWhen(Always))

    println(appWithChaos(Request(GET, "/chaos/status")).bodyString())
    println(appWithChaos(Request(GET, "/")).status)
    println(appWithChaos(Request(POST, "/chaos/activate")).bodyString())
    println(appWithChaos(Request(GET, "/")).status)
    println(appWithChaos(Request(POST, "/chaos/deactivate")).bodyString())
    println(appWithChaos(Request(GET, "/")).status)
    println(appWithChaos(Request(POST, "/chaos/activate")).bodyString())
    println(appWithChaos(Request(GET, "/")).status)
}