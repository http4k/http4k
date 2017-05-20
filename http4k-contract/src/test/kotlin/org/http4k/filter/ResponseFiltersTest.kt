package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.lens.Header.X_URI_TEMPLATE
import org.junit.Test
import java.time.Clock.systemUTC

class ResponseFiltersTest {

    @Test
    fun `reporting latency for unknown route`() {
        var called: String? = null
        val filter = ResponseFilters.ReportRouteLatency(systemUTC(), { identity, _ -> called = identity })
        val handler = filter.then { Response(OK) }

        handler(Request(Method.GET, ""))

        assertThat(called, equalTo("GET.UNMAPPED.2xx.200"))
    }

    @Test
    fun `reporting latency for known route`() {
        var called: String? = null
        val filter = ResponseFilters.ReportRouteLatency(systemUTC(), { identity, _ -> called = identity })
        val handler = filter.then { Response(OK) }

        handler(Request(Method.GET, "").with(X_URI_TEMPLATE of "/path/dir/someFile.html"))

        assertThat(called, equalTo("GET._path_dir_someFile_html.2xx.200"))
    }
}