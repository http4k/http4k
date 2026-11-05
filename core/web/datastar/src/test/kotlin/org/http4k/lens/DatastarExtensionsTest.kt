package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.datastar.DatastarEvent.PatchElements
import org.http4k.datastar.DatastarEvent.PatchSignals
import org.http4k.datastar.Element
import org.http4k.datastar.MorphMode.append
import org.http4k.datastar.MorphMode.inner
import org.http4k.datastar.Selector
import org.http4k.datastar.Signal
import org.http4k.datastar.goTo
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
    fun `can inject a datastar element with overridden defaults into a response`(approver: Approver) {
        approver.assertApproved(Response(OK).datastarElements("foo", "bar", morphMode = inner, useViewTransition = true))
    }

    @Test
    fun `can inject datastar elements into a response as HTML`(approver: Approver) {
        approver.assertApproved(Response(OK).html(PatchElements("<foo/>", "<bar/>")), TEXT_HTML)
    }

    @Test
    fun `goes to a location by appending a script to a placeholder`() {
        assertThat(
            Response(OK).datastarGoTo(Uri.of("lots/7"), Selector.of("#goto")).datastarEvents(),
            equalTo(listOf(PatchElements("""<script>window.location.assign("lots/7")</script>""", morphMode = append, selector = Selector.of("#goto"))))
        )
    }

    @Test
    fun `a location can't break out of the script`() {
        assertThat(
            Element.goTo(Uri.of("x\\\"); alert(1); (\"</script><script>alert(2)\n")).value,
            equalTo("""<script>window.location.assign("x\\\"); alert(1); (\"<\/script><script>alert(2)\n")</script>""")
        )
    }

    @Test
    fun `a script that goes to a location can be given an id, to stand in for an element`() {
        assertThat(Element.goTo(Uri.of("c/general"), id = "gate").value, equalTo("""<script id="gate">window.location.assign("c/general")</script>"""))
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
                    PatchElements("<foo/>\n<bar/>"),
                    PatchSignals(listOf(Signal.of("foo"))),
                )
            )
        )
    }
}
