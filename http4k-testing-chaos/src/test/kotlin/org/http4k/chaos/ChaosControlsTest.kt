package org.http4k.chaos

import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.chaos.ChaosBehaviour.Companion.ReturnStatus
import org.http4k.chaos.ChaosPolicy.Companion.Always
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ChaosControlsTest {

    @Test
    @Disabled
    fun `can convert a normal app to be chaotic`() {
        val app = routes("/" bind routes("/" bind GET to { Response(OK) }))

        val appWithChaos = app.withChaosControls(Always.inject(ReturnStatus(NOT_FOUND)))

        appWithChaos(Request(GET, "/chaos/status")) shouldMatch hasBody("chaos active: none")
        appWithChaos(Request(GET, "/chaos/activate")) shouldMatch hasBody("chaos active: none")
        appWithChaos(Request(POST, "/")) shouldMatch hasStatus(NOT_FOUND)
        appWithChaos(Request(GET, "/")) shouldMatch hasStatus(NOT_FOUND)
        appWithChaos(Request(POST, "/chaos/deactivate")) shouldMatch hasBody("chaos active: none")
        appWithChaos(Request(GET, "/")) shouldMatch hasStatus(OK)
        appWithChaos(Request(GET, "/chaos/activate")) shouldMatch hasBody("chaos active: none")
        appWithChaos(Request(GET, "/")) shouldMatch hasStatus(NOT_FOUND)
    }
}