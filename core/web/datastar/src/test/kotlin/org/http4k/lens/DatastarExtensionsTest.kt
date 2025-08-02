package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.DatastarEvent.PatchElements
import org.http4k.datastar.DatastarEvent.PatchSignals
import org.http4k.datastar.Signal
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class DatastarExtensionsTest {

    @Test
    fun `can inject a datastar element into a response`(approver: Approver) {
        approver.assertApproved(Response(OK).datastarElements("foo", "bar"))
    }

    @Test
    fun `can inject datastar elements into a response as HTML`(approver: Approver) {
        approver.assertApproved(Response(OK).html(PatchElements("<foo/>", "<bar/>")), TEXT_HTML)
    }

    @Test
    fun `can roundtrip events in and out of response`() {
        val response = Response(OK).datastarEvents(
            listOf(
                PatchElements("<foo/>", "<bar/>"),
                PatchSignals(listOfNotNull(Signal.of("foo"))),
            )
        )

        assertThat(
            response.datastarEvents(), equalTo(
                listOf(
                    PatchElements("<foo/>", "<bar/>"),
                    PatchSignals(listOf(Signal.of("foo"))),
                )
            )
        )
    }
}
