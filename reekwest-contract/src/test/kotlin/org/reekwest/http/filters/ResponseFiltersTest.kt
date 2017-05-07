package org.reekwest.http.filters

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.contract.ReportRouteLatency
import org.reekwest.http.contract.X_REEKWEST_ROUTE_IDENTITY
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.then
import org.reekwest.http.core.with
import java.time.Clock.systemUTC

class ResponseFiltersTest {

    @Test
    fun `reporting latency for unknown route`() {
        var called: String? = null
        val filter = ResponseFilters.ReportRouteLatency(systemUTC(), { identity, _ -> called = identity })
        val handler = filter.then { Response(OK) }

        handler(get(""))

        assertThat(called, equalTo("GET.UNMAPPED.2xx.200"))
    }

    @Test
    fun `reporting latency for known route`() {
        var called: String? = null
        val filter = ResponseFilters.ReportRouteLatency(systemUTC(), { identity, _ -> called = identity })
        val handler = filter.then { Response(OK) }

        handler(get("").with(X_REEKWEST_ROUTE_IDENTITY to "GET:/path/dir/someFile.html"))

        assertThat(called, equalTo("GET._path_dir_someFile_html.2xx.200"))
    }
}