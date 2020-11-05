package org.http4k.filter

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.throws
import org.http4k.cloudnative.RemoteRequestFailed
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_GATEWAY
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.core.Status.Companion.CONFLICT
import org.http4k.core.Status.Companion.GATEWAY_TIMEOUT
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class HandleRemoteRequestFailedTest {

    @Test
    fun `when server and client filters are used together, converts errors as expected`() {
        assertServerResponseForClientStatus(OK, hasStatus(OK))
        assertServerResponseForClientStatus(CLIENT_TIMEOUT, hasStatus(GATEWAY_TIMEOUT))
        assertServerResponseForClientStatus(GATEWAY_TIMEOUT, hasStatus(GATEWAY_TIMEOUT))

        assertServerResponseForClientStatus(BAD_REQUEST, hasStatus(SERVICE_UNAVAILABLE))
        assertServerResponseForClientStatus(CONFLICT, hasStatus(SERVICE_UNAVAILABLE))
        assertServerResponseForClientStatus(BAD_GATEWAY, hasStatus(SERVICE_UNAVAILABLE))
        assertServerResponseForClientStatus(SERVICE_UNAVAILABLE, hasStatus(SERVICE_UNAVAILABLE))
        assertServerResponseForClientStatus(I_M_A_TEAPOT, hasStatus(SERVICE_UNAVAILABLE))

        assertServerResponseForClientStatus(NOT_FOUND, hasStatus(NOT_FOUND).and(hasBody("404")))
    }

    @Test
    fun `client throws when filter fails`() {
        assertThat({
            ClientFilters.HandleRemoteRequestFailed({ false }).then { Response(NOT_FOUND) }(Request(GET, ""))
        }, throws(has(RemoteRequestFailed::status, equalTo(NOT_FOUND))))
    }

    @Test
    fun `server handles custom exception`() {
        assertThat(ServerFilters.HandleRemoteRequestFailed().then { throw CustomUpstreamFailure }(Request(GET, "")), hasStatus(SERVICE_UNAVAILABLE).and(hasBody(CustomUpstreamFailure.localizedMessage)))
    }

    @Test
    fun `multi stack errors looks sane`(approver: Approver) {
        fun stack(clientUri: String) = ServerFilters.HandleRemoteRequestFailed()
            .then(Filter { next ->
                HttpHandler {
                    next(it.uri(Uri.of(clientUri)))
                }
            })
            .then(ClientFilters.HandleRemoteRequestFailed())

        val multiStack = stack("http://parent")
            .then(stack("http://child"))
            .then { Response(INTERNAL_SERVER_ERROR).body("original error") }

        approver.assertApproved(multiStack(Request(GET, Uri.of("http://foobar/baz"))))
    }

    private fun assertServerResponseForClientStatus(input: Status, responseMatcher: Matcher<Response>) = assertThat(stackWith({ status.successful || status == NOT_FOUND }, input)(Request(GET, "")), responseMatcher)

    private fun stackWith(acceptNotFound: Response.() -> Boolean, input: Status) =
        ServerFilters.HandleRemoteRequestFailed()
            .then(ClientFilters.HandleRemoteRequestFailed(acceptNotFound))
            .then { Response(input).body(input.code.toString()) }

    private object CustomUpstreamFailure : RemoteRequestFailed(I_M_A_TEAPOT, "foo")
}
