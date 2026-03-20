package org.http4k.wiretap.junit

import App
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.greaterThan
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.wiretap.junit.RenderMode.Always
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class GlobalOTelInterceptTest {

    @RegisterExtension
    @JvmField
    val intercept = Intercept(renderMode = Always)

    @Test
    fun `traces are captured via GlobalOpenTelemetry without explicit otel wiring`(http: HttpHandler) {
        val app = App(http)
        app(Request(GET, "/test"))

        val traces = intercept.traceStore.traces()
        assertThat(traces.size, greaterThan(0))
    }

    @Test
    fun `second test also works with fresh global registration`(http: HttpHandler) {
        val app = App(http)
        app(Request(GET, "/other"))

        val traces = intercept.traceStore.traces()
        assertThat(traces.size, greaterThan(0))
    }
}
