package org.http4k.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.throws
import org.http4k.core.ContentType.Companion.APPLICATION_XML
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(XmlApprovalTest::class)
class ExampleXmlApprovalTest {

    private val app: HttpHandler = {
        Response(OK)
            .with(CONTENT_TYPE of APPLICATION_XML)
            .body("""<?xml version="1.0" encoding="UTF-8"?><UriContainer><field>foo.bar</field></UriContainer>""")
    }

    @Test
    fun `check response content`(approver: Approver) {
        approver.assertApproved(app(Request(GET, "/url")))
    }

    @Test
    fun `check response content with mismatching content type`(approver: Approver) {
        assertThat({
            approver.assertApproved(Response(OK))
        }, throws<AssertionError>())
    }

    @Test
    fun `check response content with badly-formatted XML`(approver: Approver) {
        assertThat({
            approver.assertApproved(
                Response(OK).with(CONTENT_TYPE of APPLICATION_XML).body("""<this is not really XML""")
            )
        }, throws<AssertionError>())
    }

    @Test
    fun `check request content`(approver: Approver) {
        approver.assertApproved(
            Request(GET, "/url").with(CONTENT_TYPE of APPLICATION_XML).body("""<UriContainer><field>foo.bar</field></UriContainer>""")
        )
    }
}