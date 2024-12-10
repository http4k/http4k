package org.http4k.filter

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.throws
import org.http4k.cloudnative.RemoteRequestFailed
import org.http4k.core.Filter
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.then
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(org.http4k.testing.ApprovalTest::class)
class HandleRemoteRequestFailedTest {

    @Test
    fun `when server and client filters are used together, converts errors as expected`() {
        assertServerResponseForClientStatus(Status.OK, org.http4k.hamkrest.hasStatus(Status.OK))
        assertServerResponseForClientStatus(
            Status.CLIENT_TIMEOUT,
            org.http4k.hamkrest.hasStatus(Status.GATEWAY_TIMEOUT)
        )
        assertServerResponseForClientStatus(
            Status.GATEWAY_TIMEOUT,
            org.http4k.hamkrest.hasStatus(Status.GATEWAY_TIMEOUT)
        )

        assertServerResponseForClientStatus(
            Status.BAD_REQUEST,
            org.http4k.hamkrest.hasStatus(Status.SERVICE_UNAVAILABLE)
        )
        assertServerResponseForClientStatus(Status.CONFLICT, org.http4k.hamkrest.hasStatus(Status.SERVICE_UNAVAILABLE))
        assertServerResponseForClientStatus(
            Status.BAD_GATEWAY,
            org.http4k.hamkrest.hasStatus(Status.SERVICE_UNAVAILABLE)
        )
        assertServerResponseForClientStatus(
            Status.SERVICE_UNAVAILABLE,
            org.http4k.hamkrest.hasStatus(Status.SERVICE_UNAVAILABLE)
        )
        assertServerResponseForClientStatus(
            Status.I_M_A_TEAPOT,
            org.http4k.hamkrest.hasStatus(Status.SERVICE_UNAVAILABLE)
        )

        assertServerResponseForClientStatus(
            Status.NOT_FOUND, org.http4k.hamkrest.hasStatus(Status.NOT_FOUND).and(org.http4k.hamkrest.hasBody("404")))
    }

    @Test
    fun `client throws when filter fails`() {
        assertThat({
            ClientFilters.HandleRemoteRequestFailed({ false }).then { Response(Status.NOT_FOUND) }(
                Request(
                    Method.GET,
                    ""
                )
            )
        }, throws(has(RemoteRequestFailed::status, equalTo(Status.NOT_FOUND))))
    }

    @Test
    fun `server handles custom exception`() {
        assertThat(
            ServerFilters.HandleRemoteRequestFailed().then { throw CustomUpstreamFailure }(
                Request(
                    Method.GET,
                    ""
                )
            ),
            org.http4k.hamkrest.hasStatus(Status.SERVICE_UNAVAILABLE)
                .and(org.http4k.hamkrest.hasBody(CustomUpstreamFailure.localizedMessage))
        )
    }

    @Test
    fun `multi stack errors looks sane`(approver: org.http4k.testing.Approver) {
        fun stack(clientUri: String) = ServerFilters.HandleRemoteRequestFailed()
            .then(Filter { next ->
                {
                    next(it.uri(Uri.of(clientUri)))
                }
            })
            .then(ClientFilters.HandleRemoteRequestFailed())

        val multiStack = stack("http://parent")
            .then(stack("http://child"))
            .then { Response(Status.INTERNAL_SERVER_ERROR).body("original error") }

        approver.assertApproved(multiStack(Request(Method.GET, Uri.of("http://foobar/baz"))))
    }

    private fun assertServerResponseForClientStatus(input: Status, responseMatcher: Matcher<Response>) = assertThat(
        stackWith({ status.successful || status == Status.NOT_FOUND }, input)(Request(Method.GET, "")),
        responseMatcher
    )

    private fun stackWith(acceptNotFound: Response.() -> Boolean, input: Status) =
        ServerFilters.HandleRemoteRequestFailed()
            .then(ClientFilters.HandleRemoteRequestFailed(acceptNotFound))
            .then { Response(input).body(input.code.toString()) }

    private object CustomUpstreamFailure : RemoteRequestFailed(Status.I_M_A_TEAPOT, "foo")
}
