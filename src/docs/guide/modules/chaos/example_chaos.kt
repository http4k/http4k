package guide.modules.chaos

import org.http4k.chaos.ChaosBehaviours.ReturnStatus
import org.http4k.chaos.ChaosStages.Wait
import org.http4k.chaos.ChaosTriggers.PercentageBased
import org.http4k.chaos.appliedWhen
import org.http4k.chaos.asFilter
import org.http4k.chaos.then
import org.http4k.chaos.until
import org.http4k.client.OkHttp
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.server.SunHttp
import org.http4k.server.asServer

val client = OkHttp()

fun main(args: Array<String>) {

    // chaos is split into "stages", which can be triggered by specific request or time-based criteria
    val doNothingStage = Wait.until { tx: Request -> tx.method == POST }
    val errorStage = ReturnStatus(INTERNAL_SERVER_ERROR).appliedWhen(PercentageBased(50))

    // chain the stages together with then() and finally convert to a standard http4k Filter
    val filter = doNothingStage.then(errorStage).asFilter()

    val svc: HttpHandler = { Response(OK).body("A normal response") }
    filter.then(svc).asServer(SunHttp(9000)).start().use {
        (1..10).forEach { performA(GET) }

        // this triggers the change in behaviour
        performA(POST)

        (1..10).forEach { performA(GET) }
    }
}

fun performA(method: Method) = println(method.name + " got a " + client(Request(method, "http://localhost:9000")).status)