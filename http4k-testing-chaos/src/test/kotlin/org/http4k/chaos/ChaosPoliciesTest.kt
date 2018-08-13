package org.http4k.chaos

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.chaos.ChaosBehaviours.ReturnStatus
import org.http4k.chaos.ChaosPolicies.Always
import org.http4k.chaos.ChaosPolicies.Once
import org.http4k.chaos.ChaosPolicies.Only
import org.http4k.chaos.ChaosPolicies.PercentageBased
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
import java.util.Properties

class ChaosPoliciesTest {

    @Test
    fun `Always applies by default`() {
        val http = Always.inject(ReturnStatus(INTERNAL_SERVER_ERROR)).asFilter().then { Response(OK) }
        http(Request(GET, "/foo")) shouldMatch hasStatus(INTERNAL_SERVER_ERROR).and(hasBody("")).and(hasHeader("x-http4k-chaos", "Status 500"))
        Always.toString() shouldMatch equalTo("Always")
    }

    @Test
    fun `percentage description`() {
        PercentageBased(100).toString() shouldMatch equalTo("PercentageBased (100%)")
        PercentageBased.fromEnvironment({ Properties().apply { put("CHAOS_PERCENTAGE", "75") }.getProperty(it) }).toString() shouldMatch equalTo("PercentageBased (75%)")
        PercentageBased.fromEnvironment().toString() shouldMatch equalTo("PercentageBased (50%)")
    }

    @Test
    fun `PercentageBased applied`() {
        val http = PercentageBased(100).inject(ReturnStatus(INTERNAL_SERVER_ERROR)).asFilter().then { Response(OK) }
        http(Request(GET, "/foo")) shouldMatch hasStatus(INTERNAL_SERVER_ERROR).and(hasBody("")).and(hasHeader("x-http4k-chaos", "Status 500"))
    }

    @Test
    fun `Only applies a behaviour to matching transactions`() {
        val inject = Only { it.request.method == GET }.inject(ReturnStatus(INTERNAL_SERVER_ERROR))
        inject.toString() shouldMatch equalTo("Only (trigger = (org.http4k.core.HttpTransaction) -> kotlin.Boolean) ReturnStatus (500)")
        val http = inject.asFilter().then { Response(OK) }

        http(Request(GET, "/foo")) shouldMatch hasStatus(INTERNAL_SERVER_ERROR).and(hasHeader("x-http4k-chaos", "Status 500"))
        http(Request(POST, "/bar")) shouldMatch hasStatus(OK).and(!hasHeader("x-http4k-chaos"))
        http(Request(GET, "/foo")) shouldMatch hasStatus(INTERNAL_SERVER_ERROR).and(hasHeader("x-http4k-chaos", "Status 500"))
    }

    @Test
    fun `Until stops a behaviour when triggered`() {
        val stage = Always.inject(ReturnStatus(INTERNAL_SERVER_ERROR)).until { it.request.method == POST }
        stage.toString() shouldMatch equalTo("Always ReturnStatus (500) until (org.http4k.core.HttpTransaction) -> kotlin.Boolean")

        val http = stage.asFilter().then { Response(OK) }

        http(Request(GET, "/foo")) shouldMatch hasStatus(INTERNAL_SERVER_ERROR).and(hasHeader("x-http4k-chaos", "Status 500"))
        http(Request(POST, "/bar")) shouldMatch hasStatus(OK).and(!hasHeader("x-http4k-chaos"))
        http(Request(GET, "/bar")) shouldMatch hasStatus(OK).and(!hasHeader("x-http4k-chaos"))
    }

    @Test
    fun `Once only fires once`() {
        val http = Once { it.request.method == GET }.inject(ReturnStatus(INTERNAL_SERVER_ERROR)).asFilter().then { Response(OK) }
        http(Request(POST, "/foo")) shouldMatch hasStatus(OK)
        http(Request(GET, "/foo")) shouldMatch hasStatus(INTERNAL_SERVER_ERROR)
        http(Request(POST, "/foo")) shouldMatch hasStatus(OK)
        http(Request(GET, "/foo")) shouldMatch hasStatus(OK)
    }
}