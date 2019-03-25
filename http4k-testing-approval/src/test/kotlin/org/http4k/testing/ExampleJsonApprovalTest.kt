package org.http4k.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.throws
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class ExampleJsonApprovalTest {

    private val app: HttpHandler = {
        Response(OK)
            .with(CONTENT_TYPE of APPLICATION_JSON)
            .body("""{"message":"value"}""")
    }

    @Test
    fun `check response content`(approver: Approver) {
        approver.assertApproved(app(Request(GET, "/url")))
    }


    @Test
    fun `check response content with mismatching content type`(approver: Approver) {
        assertThat({ approver.assertApproved(Response(OK)) }, throws<AssertionError>())
    }

    @Test
    fun `check response content with badly-formatted JSON`(approver: Approver) {
        assertThat({
            approver.assertApproved(Response(OK).with(CONTENT_TYPE of APPLICATION_JSON).body("foobar"))
        }, throws<AssertionError>())
    }

    @Test
    fun `check request content`(approver: Approver) {
        approver.assertApproved(
            Request(GET, "/url").with(CONTENT_TYPE of APPLICATION_JSON).body("""{"message":"value"}""")
        )
    }
}