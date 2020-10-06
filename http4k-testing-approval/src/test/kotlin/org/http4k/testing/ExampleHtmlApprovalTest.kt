package org.http4k.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.throws
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(HtmlApprovalTest::class)
class ExampleHtmlApprovalTest {

    private val app: HttpHandler = {
        Response(OK)
            .with(CONTENT_TYPE of TEXT_HTML)
            .body(
                """<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"><html><div><p>text1</p></div><div><p>text1</p></div></html>"""
            )
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
    fun `check response content with badly-formatted HTML`(approver: Approver) {
        assertThat(
            {
                approver.assertApproved(Response(OK).with(CONTENT_TYPE of TEXT_HTML).body("""<this is not really HTML"""))
            },
            throws<AssertionError>()
        )
    }
}
