package guide.reference.chaos

import org.http4k.chaos.ChaosBehaviours.ReturnStatus
import org.http4k.chaos.ChaosEngine
import org.http4k.chaos.ChaosStages.Wait
import org.http4k.chaos.ChaosTriggers.PercentageBased
import org.http4k.chaos.appliedWhen
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

fun main() {

    // chaos is split into "stages", which can be triggered by specific request or time-based criteria
    val doNothingStage = Wait.until { tx: Request -> tx.method == POST }
    val errorStage = ReturnStatus(INTERNAL_SERVER_ERROR).appliedWhen(PercentageBased(50))

    // chain the stages together with then() and create the Chaos Engine (activated)
    val engine = ChaosEngine(doNothingStage.then(errorStage)).enable()

    val svc: HttpHandler = { Response(OK).body("A normal response") }
    engine.then(svc).asServer(SunHttp(9000)).start().use {
        repeat(10) { performA(GET) }

        // this triggers the change in behaviour
        performA(POST)

        repeat(10) { performA(GET) }

        // disable the chaos
        engine.disable()

        repeat(10) { performA(GET) }
    }
}

fun performA(method: Method) = println(method.name + " got a " + client(Request(method, "http://localhost:9000")).status)
