package guide.modules.chaos

import org.http4k.chaos.ChaosBehaviours.BlockThread
import org.http4k.chaos.ChaosBehaviours.ReturnStatus
import org.http4k.chaos.ChaosPolicies.Always
import org.http4k.chaos.ChaosPolicies.Only
import org.http4k.chaos.ChaosPolicies.PercentageBased
import org.http4k.chaos.ChaosStages.Wait
import org.http4k.chaos.SwitchTrigger
import org.http4k.chaos.asFilter
import org.http4k.chaos.then
import org.http4k.chaos.until
import org.http4k.chaos.withChaosControls
import org.http4k.client.OkHttp
import org.http4k.core.HttpHandler
import org.http4k.core.HttpTransaction
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer

val client = OkHttp()

fun main(args: Array<String>) {

    // chaos is split into "stages", which can be triggered by specific request or time-based criteria
    val doNothingStage = Wait.until { tx: HttpTransaction -> tx.request.method == POST }
    val errorStage = PercentageBased(50).inject(ReturnStatus(INTERNAL_SERVER_ERROR))

    // chain the stages together with then() and finally convert to a standard http4k Filter
    val filter = doNothingStage.then(errorStage).asFilter()

    val svc: HttpHandler = { Response(OK).body("A normal response") }
    filter.then(svc).asServer(SunHttp(9000)).start().use {
        (1..10).forEach { performA(GET) }

        // this triggers the change in behaviour
        performA(POST)

        (1..10).forEach { performA(GET) }
    }

    Only(SwitchTrigger()).inject(BlockThread())

    // EXAMPLE APP WITH CHAOS CONTROLS
    val app = routes("/" bind routes("/" bind GET to { Response(OK).body("hello!") }))

    val appWithChaos = app.withChaosControls(Always.inject(ReturnStatus(Status.NOT_FOUND)))

    println(appWithChaos(Request(GET, "/chaos/status")).bodyString())
    println(appWithChaos(Request(GET, "/")).status)
    println(appWithChaos(Request(POST, "/chaos/activate")).bodyString())
    println(appWithChaos(Request(GET, "/")).status)
    println(appWithChaos(Request(POST, "/chaos/deactivate")).bodyString())
    println(appWithChaos(Request(GET, "/")).status)
    println(appWithChaos(Request(POST, "/chaos/activate")).bodyString())
    println(appWithChaos(Request(GET, "/")).status)
}

fun performA(method: Method) = println(method.name + " got a " + client(Request(method, "http://localhost:9000")).status)