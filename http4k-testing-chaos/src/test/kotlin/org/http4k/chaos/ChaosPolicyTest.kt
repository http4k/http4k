package org.http4k.chaos

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.chaos.ChaosBehaviour.Companion.ReturnStatus
import org.http4k.chaos.ChaosPolicy.Companion.Always
import org.http4k.chaos.ChaosPolicy.Companion.Only
import org.http4k.chaos.ChaosPolicy.Companion.PercentageBased
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class ChaosPolicyTest {

    @Test
    fun `Always applies by default`() {
        val http = Always.inject(ReturnStatus(INTERNAL_SERVER_ERROR)).asFilter().then { Response(OK) }
        http(Request(GET, "/foo")) shouldMatch hasStatus(INTERNAL_SERVER_ERROR).and(hasBody("")).and(hasHeader("x-http4k-chaos", "Status 500"))
    }

    @Test
    fun `PercentageBased applies by default`() {
        val http = PercentageBased(100).inject(ReturnStatus(INTERNAL_SERVER_ERROR)).asFilter().then { Response(OK) }
        http(Request(GET, "/foo")) shouldMatch hasStatus(INTERNAL_SERVER_ERROR).and(hasBody("")).and(hasHeader("x-http4k-chaos", "Status 500"))
    }

    @Test
    fun `Only applies a behaviour to matching transactions`() {
        val http = Only { it.request.method == GET }.inject(ReturnStatus(INTERNAL_SERVER_ERROR)).asFilter().then { Response(OK) }

        http(Request(GET, "/foo")) shouldMatch hasStatus(INTERNAL_SERVER_ERROR).and(hasHeader("x-http4k-chaos", "Status 500"))
        http(Request(POST, "/bar")) shouldMatch hasStatus(OK).and(!hasHeader("x-http4k-chaos"))
        http(Request(GET, "/foo")) shouldMatch hasStatus(INTERNAL_SERVER_ERROR).and(hasHeader("x-http4k-chaos", "Status 500"))
    }

    @Test
    fun `Until stops a behaviour when triggered`() {
        val http = Always.inject(ReturnStatus(INTERNAL_SERVER_ERROR)).until { it.request.method == POST }.asFilter().then { Response(OK) }

        http(Request(GET, "/foo")) shouldMatch hasStatus(INTERNAL_SERVER_ERROR).and(hasHeader("x-http4k-chaos", "Status 500"))
        http(Request(POST, "/bar")) shouldMatch hasStatus(OK).and(!hasHeader("x-http4k-chaos"))
        http(Request(GET, "/bar")) shouldMatch hasStatus(OK).and(!hasHeader("x-http4k-chaos"))
    }
}