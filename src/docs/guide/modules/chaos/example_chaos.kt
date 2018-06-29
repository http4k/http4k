package guide.modules.chaos

import org.http4k.chaos.ChaosBehaviour.Companion.ReturnStatus
import org.http4k.chaos.ChaosPolicy.Companion.PercentageBased
import org.http4k.chaos.ChaosStage.Companion.Wait
import org.http4k.client.OkHttp
import org.http4k.core.HttpHandler
import org.http4k.core.HttpTransaction
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
}

fun performA(method: Method) = println(method.name + " got a " + client(Request(method, "http://localhost:9000")).status)