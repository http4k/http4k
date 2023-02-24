package org.http4k.tracing.renderer

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.http4k.tracing.TraceRenderer
import org.http4k.tracing.entire_trace_1
import org.http4k.tracing.entire_trace_2
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
abstract class TraceRendererContract(
    private val title: String,
    private val format: String,
    private val renderer: TraceRenderer
) {
    @Test
    fun `renders as expected`(approver: Approver) {
        val render = renderer.render("foobar", listOf(entire_trace_1, entire_trace_2))
        approver.assertApproved(render.content)
        assertThat(render.title, equalTo(title))
        assertThat(render.format, equalTo(format))
    }
}