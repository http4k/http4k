package org.reekwest.http.filters

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.then

class RequestTracingTest {

    @Test
    fun `request traces are copied from inbound to outbound requests`() {

        val traces = ZipkinTraces(TraceId.new(), TraceId.new(), TraceId.new())

        val client: HttpHandler = ClientFilters.RequestTracing.then {
            assertThat(ZipkinTraces(it), equalTo(traces))
            Response(OK)
        }
        val simpleProxyServer: HttpHandler = ServerFilters.RequestTracing.then { client(get("/somePath")) }

        val response = simpleProxyServer(ZipkinTraces(traces, get("")))

        assertThat(ZipkinTraces(response), equalTo(traces))
    }

}