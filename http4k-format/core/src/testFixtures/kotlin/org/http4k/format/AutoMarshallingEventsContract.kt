package org.http4k.format

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.events.AutoMarshallingEvents
import org.http4k.events.EventFilters
import org.http4k.events.MyEvent
import org.http4k.events.plus
import org.http4k.events.then
import org.http4k.filter.TraceId
import org.http4k.filter.ZipkinTraces
import org.http4k.filter.ZipkinTracesStorage
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.util.FixedClock
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.StringWriter

@ExtendWith(ApprovalTest::class)
abstract class AutoMarshallingEventsContract(private val j: AutoMarshalling) {

    @Test
    fun `event serialises to library format`(approver: Approver) {
        val final = MyEvent() + ("first" to "1") + ("second" to 2)
        val w = StringWriter()

        ZipkinTracesStorage.THREAD_LOCAL.setForCurrentThread(ZipkinTraces(
            TraceId("61a2ebc48552f603"),
            TraceId("f494afd6389ff8f0"),
            TraceId("b429b2d2803523b9")
        ))

        val pipeline = EventFilters.AddZipkinTraces()
            .then(EventFilters.AddTimestamp(FixedClock))
            .then(AutoMarshallingEvents(j, w::write))

        pipeline(final)

        approver.assertApproved(Response(OK).with(CONTENT_TYPE of APPLICATION_JSON).body(w.toString()))
    }
}
