package org.http4k.testing

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class BobTest {

    val app: HttpHandler = { Response(OK).body("hello world") }

    @Test
    fun `foo bar`(approver: Approver) {
        approver(hasStatus(OK)) {
            app(Request(Method.GET, ""))
        }
    }
}