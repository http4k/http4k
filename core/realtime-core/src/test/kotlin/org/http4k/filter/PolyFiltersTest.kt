package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.events.ProtocolEvent
import org.http4k.lens.accept
import org.http4k.routing.bind
import org.http4k.routing.poly
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.testing.RecordingEvents
import org.http4k.testing.toHttpHandler
import org.junit.jupiter.api.Test

class PolyFiltersTest {

    @Test
    fun `a request that falls through from sse to http is reported once, as http`() {
        val events = RecordingEvents()
        val app = PolyFilters.ReportTransaction(events).then(
            poly(
                "/page" bind GET to { Response(OK) },
                "/live" bind sse { it.close() },
            )
        )

        val response = app.toHttpHandler()(Request(GET, "/page").accept(TEXT_EVENT_STREAM))

        assertThat(response.status, equalTo(OK))
        assertThat(events.map { (it as? ProtocolEvent)?.protocol }, equalTo(listOf<String?>("http")))
    }
}
