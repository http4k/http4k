package reference.chaos

import org.http4k.chaos.ChaosBehaviours.ReturnStatus
import org.http4k.chaos.ChaosEngine
import org.http4k.chaos.ChaosTriggers.Once
import org.http4k.chaos.appliedWhen
import org.http4k.chaos.withChaosApi
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes

class FakeSystem : HttpHandler {
    private val chaosEngine = ChaosEngine()

    private val app = routes("/hello" bind GET to { Response(OK) }).withChaosApi(chaosEngine)

    fun blowUpOnce() {
        chaosEngine.enable(ReturnStatus(I_M_A_TEAPOT).appliedWhen(Once()))
    }

    fun relax() {
        chaosEngine.disable()
    }

    override fun invoke(p1: Request) = app(p1)
}

fun main() {
    val fakeSystem = FakeSystem()
    println(fakeSystem(Request(GET, "/hello")))

    fakeSystem.blowUpOnce()
    println(fakeSystem(Request(GET, "/hello")))
    println(fakeSystem(Request(GET, "/hello")))

    fakeSystem.blowUpOnce()
    fakeSystem.relax()
    println(fakeSystem(Request(GET, "/hello")))
}
