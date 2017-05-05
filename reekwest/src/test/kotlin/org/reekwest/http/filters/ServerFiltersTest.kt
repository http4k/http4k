package org.reekwest.http.filters

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.junit.Test
import org.reekwest.http.core.Request
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status
import org.reekwest.http.core.then

class ServerFiltersTest {

    @Test
    fun `initialises request tracing and sets on outgoing response when not present`() {
        ZipkinTraces.THREAD_LOCAL.remove()

        var newThreadLocal: ZipkinTraces? = null
        val svc = ServerFilters.RequestTracing.then {
            newThreadLocal = ZipkinTraces.THREAD_LOCAL.get()
            assertThat(ZipkinTraces.THREAD_LOCAL.get(), present())
            Response(Status.OK)
        }

        assertThat(ZipkinTraces(svc(Request.get(""))), equalTo(newThreadLocal))
    }

    @Test
    fun `uses existing request tracing from request and sets on outgoing response not present`() {
        val traces = ZipkinTraces(TraceId.new(), TraceId.new(), TraceId.new())

        ZipkinTraces.THREAD_LOCAL.remove()
        val svc = ServerFilters.RequestTracing.then {
            assertThat(ZipkinTraces.THREAD_LOCAL.get(), present())
            Response(Status.OK)
        }

        val actual = svc(ZipkinTraces(traces, get("")))
        assertThat(ZipkinTraces(actual), equalTo(traces))
    }
}
