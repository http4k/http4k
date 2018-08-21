package org.http4k.chaos

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.chaos.ChaosBehaviours.ReturnStatus
import org.http4k.chaos.ChaosStages.Wait
import org.http4k.chaos.ChaosTriggers.Always
import org.http4k.contract.ApiKey
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.Header
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test

class ChaosControlsTest {

    private val noChaos = """{"chaos":"none"}"""
    private val originalChaos = """{"chaos":"Always ReturnStatus (404)"}"""
    private val customChaos = """{"chaos":"Always ReturnStatus (418)"}"""

    @Test
    fun `can convert a normal app to be chaotic`() {
        val app = routes("/" bind GET to { Response(OK) })

        val appWithChaos = app.withChaosControls(ReturnStatus(NOT_FOUND).appliedWhen(Always))

        appWithChaos(Request(GET, "/chaos/status")) shouldMatch hasBody(noChaos)
        appWithChaos(Request(POST, "/chaos/activate")) shouldMatch hasStatus(OK).and(hasBody(originalChaos))
        appWithChaos(Request(GET, "/chaos/status")) shouldMatch hasBody(originalChaos)
        appWithChaos(Request(POST, "/")) shouldMatch hasStatus(NOT_FOUND)
        appWithChaos(Request(GET, "/")) shouldMatch hasStatus(NOT_FOUND)
        appWithChaos(Request(POST, "/chaos/deactivate")) shouldMatch hasStatus(OK).and(hasBody(noChaos))
        appWithChaos(Request(GET, "/chaos/status")) shouldMatch hasBody(noChaos)
        appWithChaos(Request(GET, "/")) shouldMatch hasStatus(OK)
        appWithChaos(Request(POST, "/chaos/activate/new").body("""
            [{
                "type":"policy",
                "policy": {
                    "type":"always"
                },
                "behaviour":{
                    "type":"status",
                    "status":418
                }
            }]""".trimIndent())) shouldMatch hasStatus(OK).and(hasBody(customChaos))
        appWithChaos(Request(GET, "/chaos/status")) shouldMatch hasBody(customChaos)
        appWithChaos(Request(GET, "/")) shouldMatch hasStatus(I_M_A_TEAPOT)
        appWithChaos(Request(POST, "/chaos/deactivate")) shouldMatch hasStatus(OK).and(hasBody(noChaos))
        appWithChaos(Request(GET, "/chaos/status")) shouldMatch hasBody(noChaos)
        appWithChaos(Request(POST, "/chaos/activate")) shouldMatch hasStatus(OK).and(hasBody(customChaos))
    }

    @Test
    fun `can configure chaos controls`() {
        val app = routes("/" bind GET to { Response(OK) })

        val appWithChaos = app.withChaosControls(
                Wait,
                ApiKey(Header.required("secret"), { true }),
                "/context"
        )

        appWithChaos(Request(GET, "/context/status")) shouldMatch hasStatus(UNAUTHORIZED)
        appWithChaos(Request(GET, "/context/status").header("secret", "whatever")) shouldMatch hasStatus(OK)
    }
}