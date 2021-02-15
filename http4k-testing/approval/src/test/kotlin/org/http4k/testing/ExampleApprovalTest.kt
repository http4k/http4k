package org.http4k.testing

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class ExampleApprovalTest {

    private val app: HttpHandler = { Response(OK).body("hello world") }

    @Test
    fun `check response content`(approver: Approver) {
        approver.assertApproved(app(Request(GET, "/url")))
    }

    @Test
    fun `check request content`(approver: Approver) {
        approver.assertApproved(Request(GET, "/url").body("foobar"))
    }

    @Test
    fun `create hamkrest matcher`(approver: Approver) {
        assertThat(app(Request(GET, "/url")), hasStatus(OK).and(approver.hasApprovedContent()))
    }
}
