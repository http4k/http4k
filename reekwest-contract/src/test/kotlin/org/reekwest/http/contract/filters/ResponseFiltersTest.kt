package org.reekwest.http.contract.filters

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.contract.X_REEKWEST_ROUTE_IDENTITY
import org.reekwest.http.contract.filters.ResponseFilters.ReportLatency
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.then
import org.reekwest.http.core.with
import org.reekwest.http.util.TickingClock
import java.time.Duration

class ResponseFiltersTest {

    @Test
    fun `reporting latency for unknown route`() {
        var called: Pair<String, Duration>? = null
        val filter = ReportLatency(TickingClock, { identity, duration -> called = identity to duration })
        val handler = filter.then { Response(OK) }

        handler(get(""))

        assertThat(called, equalTo("GET.UNMAPPED.2xx.200" to Duration.ofSeconds(1)))
    }

    @Test
    fun `reporting latency for known route`() {
        var called: Pair<String, Duration>? = null
        val filter = ReportLatency(TickingClock, { identity, duration -> called = identity to duration })
        val handler = filter.then { Response(OK) }

        handler(get("").with(X_REEKWEST_ROUTE_IDENTITY to "GET:/path/dir/someFile.html"))

        assertThat(called, equalTo("GET._path_dir_someFile_html.2xx.200" to Duration.ofSeconds(1)))
    }
}