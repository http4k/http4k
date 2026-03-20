package org.http4k.wiretap.junit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.wiretap.junit.RenderMode.Always
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class SimpleInterceptTest {

    private val app = routes("/" bind GET to { Response(OK).body("hello") })

    @RegisterExtension
    @JvmField
    val intercept = Intercept(app, Always)

    @Test
    fun `requests through httpHandler reach the original app`(http: HttpHandler) {
        val response = http(Request(GET, "/"))
        assertThat(response.bodyString(), equalTo("hello"))
    }

    @Test
    fun `otel traces are recorded when requests pass through`(http: HttpHandler) {
        http(Request(GET, "/"))

        val traces = intercept.traceStore.traces()
        assertThat(traces.size, greaterThan(0))
    }
}
