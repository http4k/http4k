package org.http4k.format

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.events.AutoJsonEvents
import org.http4k.events.EventFilters
import org.http4k.events.MyEvent
import org.http4k.events.plus
import org.http4k.events.then
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.StringWriter
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@ExtendWith(JsonApprovalTest::class)
abstract class AutoJsonEventsContract(private val j: AutoMarshallingJson) {

    @Test
    fun `event serialises to json`(approver: Approver) {
        val final = MyEvent() + ("first" to "1") + ("second" to 2)
        val w = StringWriter()

        val pipeline = EventFilters.AddZipkinTraces()
            .then(EventFilters.AddTimestamp(Clock.fixed(Instant.EPOCH, ZoneId.systemDefault())))
            .then(AutoJsonEvents(j, w::write))

        pipeline(final)

        approver.assertApproved(Response(OK).with(CONTENT_TYPE of APPLICATION_JSON).body(w.toString()))
    }
}