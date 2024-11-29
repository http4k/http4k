package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.DatastarEvent
import org.http4k.datastar.DatastarEvent.MergeFragments
import org.http4k.datastar.DatastarEvent.MergeSignals
import org.http4k.datastar.Signal
import org.http4k.sse.SseMessage
import org.http4k.sse.SseMessage.Event
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class DatastarExtensionsTest {

    @Test
    fun `can inject a datastar fragment into a response`(approver: Approver) {
        approver.assertApproved(Response(OK).datastarFragments("foo", "bar"))
    }

    @Test
    fun `can inject datastar fragments into a response as HTML`(approver: Approver) {
        approver.assertApproved(Response(OK).html(MergeFragments("<foo/>", "<bar/>")), TEXT_HTML)
    }

    @Test
    fun `can roundtrip events in and out of response`() {
        val response = Response(OK).datastarEvents(
            listOfNotNull(
                MergeFragments("<foo/>", "<bar/>"),
                MergeSignals(listOfNotNull(Signal.of("foo"))),
            )
        )

        assertThat(
            response.datastarEvents(), equalTo(
                listOf(
                    MergeFragments("<foo/>", "<bar/>"),
                    MergeSignals(listOf(Signal.of("foo"))),
                )
            )
        )
    }
}

fun Response.datastarEvents(events: List<DatastarEvent>) =
    contentType(TEXT_EVENT_STREAM).body(events.joinToString("\n\n") { it.toSseEvent().toMessage() })

private fun Response.datastarEvents() =
    bodyString().split("\n\n")
        .filter { it.isNotBlank() }
        .map { SseMessage.parse(it) }
        .filterIsInstance<Event>()
        .map { DatastarEvent.from(it)}
