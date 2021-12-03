package org.http4k.chaos

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.chaos.ChaosBehaviours.ReturnStatus
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class ChaoticHttpHandlerTest {

    private val fake = object : ChaoticHttpHandler() {
        override val app: HttpHandler = { Response(OK) }
    }

    @Test
    fun `handles spurious errors`() {
        val fake = object : ChaoticHttpHandler() {
            override val app: HttpHandler = { error("foobar") }
        }
        assertThat(fake(Request(Method.GET, "")), hasStatus(INTERNAL_SERVER_ERROR))
    }

    @Test
    fun `can enable and disable behaviour`() {

        assertThat(fake(Request(Method.GET, "")), hasStatus(OK))

        fake.misbehave(ReturnStatus(INTERNAL_SERVER_ERROR))
        assertThat(fake(Request(Method.GET, "")), hasStatus(INTERNAL_SERVER_ERROR))

        fake.behave()
        assertThat(fake(Request(Method.GET, "")), hasStatus(OK))
    }

    @Test
    fun `can start on a default port`() {
        fake.start().use {
            assertThat(fake(Request(Method.GET, "http://localhost:${it.port()}")), hasStatus(OK))
        }
    }
}
