package org.http4k.hotreload

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.contentType
import org.http4k.testing.Approver
import org.http4k.testing.HtmlApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration

@ExtendWith(HtmlApprovalTest::class)
class HotReloadRoutesTest {

    @Test
    fun `proxies to backend on non HTML route`() = runBlocking {
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

    @Test
    fun `responds to ping`() = runBlocking {
        val app = HotReloadRoutes({ Response(INTERNAL_SERVER_ERROR) })
        assertThat(app(Request(GET, "/http4k/ping")), hasStatus(OK).and(hasBody("pong")))
    }

    @Test
    fun `responds to hot-reload after sleeping`() = runBlocking {
        val duration = Duration.ofMinutes(1111)
        val app = HotReloadRoutes({ Response(INTERNAL_SERVER_ERROR) }, duration, {
            assertThat(it, equalTo(duration))
        })
        assertThat(app(Request(GET, "/http4k/hot-reload")), hasStatus(OK).and(hasBody("")))
    }
}
