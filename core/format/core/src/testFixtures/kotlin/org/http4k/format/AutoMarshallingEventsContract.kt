package org.http4k.format

import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.events.AutoMarshallingEvents
import org.http4k.events.EventFilters
import org.http4k.events.HttpEvent
import org.http4k.events.MyEvent
import org.http4k.events.SseEvent
import org.http4k.events.WsEvent
import org.http4k.events.plus
import org.http4k.events.then
import org.http4k.filter.TraceId
import org.http4k.filter.ZipkinTraces
import org.http4k.filter.ZipkinTracesStorage
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.http4k.util.FixedClock
import org.http4k.websocket.WsStatus.Companion.BUGGYCLOSE
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.StringWriter

@ExtendWith(ApprovalTest::class)
abstract class AutoMarshallingEventsContract(private val j: AutoMarshalling) {

    @Test
    fun `event serialises to library format`(approver: Approver) {
        val final = MyEvent() + ("first" to "1") + ("second" to 2)
        val w = StringWriter()

        ZipkinTracesStorage.THREAD_LOCAL.setForCurrentThread(
            ZipkinTraces(
                TraceId("61a2ebc48552f603"),
                TraceId("f494afd6389ff8f0"),
                TraceId("b429b2d2803523b9")
            )
        )

        val pipeline = EventFilters.AddZipkinTraces()
            .then(EventFilters.AddTimestamp(FixedClock))
            .then(AutoMarshallingEvents(j, w::write))

        pipeline(final)

        approver.assertApproved(w.toString())
    }

    @Test
    fun `http protocol events serialises to library format`(approver: Approver) {
        val w = StringWriter()

        AutoMarshallingEvents(extendedMarshaller(), w::write)(HttpEvent.Incoming(Uri.of("uri"), GET, OK, 1000, "template"))

        approver.assertApproved(w.toString())
    }

    @Test
    fun `ws protocol events serialises to library format`(approver: Approver) {
        val w = StringWriter()

        AutoMarshallingEvents(extendedMarshaller(), w::write)(WsEvent.Incoming(Uri.of("uri"), GET, BUGGYCLOSE, 1000, "template"))

        approver.assertApproved(w.toString())
    }

    @Test
    fun `sse protocol events serialises to library format`(approver: Approver) {
        val w = StringWriter()

        AutoMarshallingEvents(extendedMarshaller(), w::write)(SseEvent.Incoming(Uri.of("uri"), GET, OK, 1000, "template"))

        approver.assertApproved(w.toString())
    }

    abstract fun extendedMarshaller(): AutoMarshalling

}
