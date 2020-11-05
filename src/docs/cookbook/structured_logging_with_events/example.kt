package cookbook.structured_logging_with_events

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.events.AutoJsonEvents
import org.http4k.events.Event
import org.http4k.events.EventFilter
import org.http4k.events.EventFilters
import org.http4k.events.plus
import org.http4k.events.then
import org.http4k.filter.ResponseFilters
import org.http4k.format.Jackson

fun main() {
    // Stack filters for Events in the same way as HttpHandlers to transform or add metadata to the Events.
    // We use AutoJsonEvents (here with Jackson) to handle the final serialisation process.
    val events =
        EventFilters.AddTimestamp()
            .then(EventFilters.AddZipkinTraces())
            .then(AddRequestCount())
            .then(AutoJsonEvents(Jackson))

    val app = HttpHandler { _: Request -> Response(OK).body("hello") }

    val appWithEvents =
        ResponseFilters.ReportHttpTransaction {
            // to "emit" an event, just invoke() the Events!
            events(IncomingHttpRequest(it.request.uri, it.response.status.code, it.duration.toMillis()))
        }.then(app)

    appWithEvents(Request(GET, "/path1"))
    appWithEvents(Request(GET, "/path2"))
}

// this is our custom event which will be printed in a structured way
data class IncomingHttpRequest(val uri: Uri, val status: Int, val duration: Long) : Event

// here is a new EventFilter that adds custom metadata to the emitted events
fun AddRequestCount(): EventFilter {
    var requestCount = 0
    return EventFilter { next ->
        {
            next(it + ("requestCount" to requestCount++))
        }
    }
}
