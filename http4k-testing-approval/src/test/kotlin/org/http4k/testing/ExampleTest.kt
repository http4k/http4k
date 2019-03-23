package org.http4k.testing

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class ExampleTest {

    private val app: HttpHandler = { Response(OK).body("hello world") }

    @Test
    fun `check response content`(approver: Approver) {
        approver {
            app(Request(GET, "/url"))
        }
    }

    @Test
    fun `check request content`(approver: Approver) {
        approver {
            Request(GET, "/url").body("foobar")
        }
    }
}