package org.http4k.testing

import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith


class ExampleHtml5ApprovalTest {
    val response = Response(OK)
        .with(CONTENT_TYPE of TEXT_HTML)
        .body("""<!doctype html><html><div><p>text1</p></div><div><p>text1</p></div></html>""")
    
    @Test
    @ExtendWith(Html5ApprovalTest::class)
    fun `check response content`(approver: Approver) {
        approver.assertApproved(response)
    }
    
    @Test
    @ExtendWith(Html5MessageApprovalTest::class)
    fun `check entire message with HTML5 content`(approver: Approver) {
        approver.assertApproved(response)
    }
    
    @Test
    @ExtendWith(Html5ApprovalTest::class)
    fun `check response content with mismatching content type`(approver: Approver) {
        assertThrows<AssertionError> {
            approver.assertApproved(response.with(CONTENT_TYPE of TEXT_PLAIN))
        }
    }
    
    @Test
    @ExtendWith(Html5ApprovalTest::class)
    fun `accepts invalid HTML`(approver: Approver) {
        approver.assertApproved(response.body("""<this is not really HTML"""))
    }
}
