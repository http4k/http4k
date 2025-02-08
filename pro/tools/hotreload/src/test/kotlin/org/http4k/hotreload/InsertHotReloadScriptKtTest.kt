package org.http4k.hotreload

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.hamkrest.hasBody
import org.http4k.lens.contentType
import org.http4k.testing.Approver
import org.http4k.testing.HtmlApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(HtmlApprovalTest::class)
class InsertHotReloadScriptKtTest {

    @Test
    fun `inserts script into HTML`(approver: Approver) {
        val app = InsertHotReloadScript("/foo").then { req: Request ->
            Response(Status.OK).contentType(TEXT_HTML)
                .body("<html><head></head><body></body></html>")
        }

        approver.assertApproved(app(Request(GET, "/foo")))
    }

    @Test
    fun `ignores non HTML`(approver: Approver) {
        val app = InsertHotReloadScript("/foo").then { req: Request -> Response(Status.OK).body("foo") }
        assertThat(app(Request(GET, "")), hasBody("foo"))
    }
}
