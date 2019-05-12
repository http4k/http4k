package org.http4k.chaos

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo

import org.http4k.chaos.ChaosBehaviours.ReturnStatus
import org.http4k.chaos.ChaosStages.Wait
import org.http4k.chaos.ChaosTriggers.Always
import org.http4k.contract.security.ApiKeySecurity
import org.http4k.contract.security.NoSecurity
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.UriTemplate
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.Header
import org.http4k.routing.RoutedResponse
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.junit.jupiter.api.Test

class ChaosEngineTest {

    private val noChaos = """{"chaos":"none"}"""
    private val originalChaos = """{"chaos":"Always ReturnStatus (404)"}"""
    private val customChaos = """{"chaos":"Always ReturnStatus (418)"}"""

    @Test
    fun `can convert a normal app to be chaotic`() {
        val app = routes("/" bind GET to { Response(OK) })

        val appWithChaos = app.withChaosEngine(ReturnStatus(NOT_FOUND).appliedWhen(Always()))

        assertThat(appWithChaos(Request(GET, "/chaos/status")), hasBody(noChaos))
        assertThat(appWithChaos(Request(POST, "/chaos/activate")), hasStatus(OK).and(hasBody(originalChaos)))
        assertThat(appWithChaos(Request(GET, "/chaos/status")), hasBody(originalChaos))
        assertThat(appWithChaos(Request(POST, "/")), hasStatus(NOT_FOUND))
        assertThat(appWithChaos(Request(GET, "/")), hasStatus(NOT_FOUND))
        assertThat(appWithChaos(Request(POST, "/chaos/deactivate")), hasStatus(OK).and(hasBody(noChaos)))
        assertThat(appWithChaos(Request(GET, "/chaos/status")), hasBody(noChaos))
        assertThat(appWithChaos(Request(GET, "/")), hasStatus(OK))
        assertThat(appWithChaos(Request(POST, "/chaos/activate/new").body("""
                   [{
                       "type":"trigger",
                       "trigger": {
                           "type":"always"
                       },
                       "behaviour":{
                           "type":"status",
                           "status":418
                       }
                   }]""".trimIndent())), hasStatus(OK).and(hasBody(customChaos)))
        assertThat(appWithChaos(Request(GET, "/chaos/status")), hasBody(customChaos))
        assertThat(appWithChaos(Request(GET, "/")), hasStatus(I_M_A_TEAPOT))
        assertThat(appWithChaos(Request(POST, "/chaos/deactivate")), hasStatus(OK).and(hasBody(noChaos)))
        assertThat(appWithChaos(Request(GET, "/chaos/status")), hasBody(noChaos))
        assertThat(appWithChaos(Request(POST, "/chaos/activate")), hasStatus(OK).and(hasBody(customChaos)))
    }

    @Test
    fun `can configure chaos controls`() {
        val app = routes("/" bind GET to { Response(OK) })

        val appWithChaos = app.withChaosEngine(
                Wait,
            ApiKeySecurity(Header.required("secret"), { true }),
                "/context"
        )

        assertThat(appWithChaos(Request(GET, "/context/status")), hasStatus(UNAUTHORIZED))
        assertThat(appWithChaos(Request(GET, "/context/status").header("secret", "whatever")), hasStatus(OK))
    }

    @Test
    fun `combines with other route blocks`() {
        val app = routes("/{bib}/{bar}" bind GET to { Response(I_M_A_TEAPOT).body(it.path("bib")!! + it.path("bar")!!) })

        val appWithChaos = app.withChaosEngine(
                Wait,
            NoSecurity,
                "/context"
        )

        assertThat(appWithChaos(Request(GET, "/context/status")), hasStatus(OK))

        val routed = appWithChaos(Request(GET, "/foo/bob"))
        assertThat(routed, hasStatus(I_M_A_TEAPOT).and(hasBody("foobob")))
        assertThat((routed as RoutedResponse).xUriTemplate, equalTo(UriTemplate.from("{bib}/{bar}")))
    }

    @Test
    fun `combines with a standard handler route blocks`() {
        val app = { _: Request -> Response(I_M_A_TEAPOT) }

        val appWithChaos = app.withChaosEngine(
            Wait,
            NoSecurity,
            "/context"
        )

        assertThat(appWithChaos(Request(GET, "/context/status")), hasStatus(OK))

        val routed = appWithChaos(Request(GET, "/foo/bob"))
        assertThat(routed, hasStatus(I_M_A_TEAPOT))
        assertThat((routed as RoutedResponse).xUriTemplate, equalTo(UriTemplate.from("{path:.*}")))
    }
}