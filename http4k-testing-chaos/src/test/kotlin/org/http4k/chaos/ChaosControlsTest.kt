package org.http4k.chaos

import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.chaos.ChaosBehaviour.Companion.ReturnStatus
import org.http4k.chaos.ChaosPolicy.Companion.Always
import org.http4k.chaos.ChaosStage.Companion.Wait
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test

class ChaosControlsTest {

    private val expectedChaos = "chaos active: Repeat [Wait until SwitchTrigger (active = true)] then [Always ReturnStatus (404)] until NOT SwitchTrigger (active = true)"
    private val noChaos = "chaos active: none"

    @Test
    fun `can convert a normal app to be chaotic`() {
        val app = routes("/" bind GET to { Response(OK) })

        val appWithChaos = app.withChaosControls(Always.inject(ReturnStatus(NOT_FOUND)))

        appWithChaos(Request(GET, "/chaos/status")) shouldMatch hasBody(noChaos)
        appWithChaos(Request(POST, "/chaos/activate")) shouldMatch hasBody(expectedChaos)
        appWithChaos(Request(GET, "/chaos/status")) shouldMatch hasBody(expectedChaos)
        appWithChaos(Request(POST, "/")) shouldMatch hasStatus(NOT_FOUND)
        appWithChaos(Request(GET, "/")) shouldMatch hasStatus(NOT_FOUND)
        appWithChaos(Request(POST, "/chaos/deactivate")) shouldMatch hasBody(noChaos)
        appWithChaos(Request(GET, "/chaos/status")) shouldMatch hasBody(noChaos)
        appWithChaos(Request(GET, "/")) shouldMatch hasStatus(OK)
        appWithChaos(Request(POST, "/chaos/activate")) shouldMatch hasBody(expectedChaos)
        appWithChaos(Request(GET, "/chaos/status")) shouldMatch hasBody(expectedChaos)
        appWithChaos(Request(GET, "/")) shouldMatch hasStatus(NOT_FOUND)
    }

    @Test
    fun `can configure chaos controls`() {
        val app = routes("/" bind GET to { Response(OK) })

        val appWithChaos = app.withChaosControls(
                Wait,
                Filter { next ->
                    { it.header("secret")?.run { next(it) } ?: Response(UNAUTHORIZED) }
                },
                "/context"
        )

        appWithChaos(Request(GET, "/context/status")) shouldMatch hasStatus(UNAUTHORIZED)
        appWithChaos(Request(GET, "/context/status").header("secret", "whatever")) shouldMatch hasStatus(OK)
    }
}