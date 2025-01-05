package org.http4k.hotreload

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.contentType
import org.http4k.testing.Approver
import org.http4k.testing.HtmlApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(HtmlApprovalTest::class)
class HotReloadRoutesTest {

    @Test
    fun `proxies to backend on non HTML route`() {
        val app = HotReloadRoutes({ Response(OK).body("fallback") })
        assertThat(app(Request(GET, "")), hasStatus(OK).and(hasBody("fallback")))
    }

    @Test
    fun `inserts script into proxied HTML`(approver: Approver) {
        val app = HotReloadRoutes({
            Response(OK).contentType(TEXT_HTML)
                .body("<html><head></head><body></body></html>")
        })

        approver.assertApproved(app(Request(GET, "/foo")))
    }
}
