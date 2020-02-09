package org.http4k.chaos

import org.http4k.chaos.ChaosBehaviours.ReturnStatus
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.then
import org.http4k.routing.bind
import org.http4k.routing.routes

class FakeSystem : HttpHandler {
    private val chaosEngine = ChaosEngine(initialPosition = false)

    private val app = chaosEngine.then(routes("/{word}" bind GET to { Response(Status.OK) }))

    fun blowUp() {
        chaosEngine.toggle(true)
        chaosEngine.update(ReturnStatus(I_M_A_TEAPOT))
    }

    fun relax() {
        chaosEngine.toggle(false)
    }

    override fun invoke(p1: Request) = app(p1)
}

fun main() {
    val fakeSystem = FakeSystem()
    println(fakeSystem(Request(GET, "/hello")))
    fakeSystem.blowUp()
    println(fakeSystem(Request(GET, "/hello")))
    fakeSystem.relax()
    println(fakeSystem(Request(GET, "/hello")))
}
