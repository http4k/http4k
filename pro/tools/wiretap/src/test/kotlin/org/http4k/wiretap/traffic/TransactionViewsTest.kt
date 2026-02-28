package org.http4k.wiretap.traffic

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.http4k.wiretap.domain.Direction.Inbound
import org.http4k.wiretap.domain.HeaderEntry
import org.http4k.wiretap.domain.WiretapTransaction
import org.http4k.wiretap.domain.toDetail
import org.http4k.wiretap.domain.toSummary
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration
import java.time.Instant

@ExtendWith(JsonApprovalTest::class)
class TransactionViewsTest {

    private fun tx(
        uri: Uri = Uri.of("/foo"),
        request: Request = Request(GET, uri),
        response: Response = Response(OK)
    ) = WiretapTransaction(
        id = 1,
        transaction = HttpTransaction(
            request = request,
            response = response,
            duration = Duration.ofMillis(100),
            start = Instant.EPOCH
        ),
        direction = Inbound
    )

    @Test
    fun `row path shows only path when no query string`() {
        val view = TransactionRowView(tx(uri = Uri.of("/foo")).toSummary())
        assertThat(view.tx.path, equalTo("/foo"))
    }

    @Test
    fun `row path includes query string when present`() {
        val view = TransactionRowView(tx(uri = Uri.of("/foo?bar=baz&qux=1")).toSummary())
        assertThat(view.tx.path, equalTo("/foo?bar=baz&qux=1"))
    }

    @Test
    fun `detail view has no query params when query is empty`() {
        val view = TransactionDetailView(tx(uri = Uri.of("/foo")).toDetail())
        assertThat(view.tx.queryParams, equalTo(emptyList()))
    }

    @Test
    fun `detail view parses query params`() {
        val view = TransactionDetailView(tx(uri = Uri.of("/foo?bar=baz&qux=1")).toDetail())
        assertThat(
            view.tx.queryParams, equalTo(
                listOf(
                    HeaderEntry("bar", "baz"),
                    HeaderEntry("qux", "1")
                )
            )
        )
    }

    @Test
    fun `detail view handles query param without value`() {
        val view = TransactionDetailView(tx(uri = Uri.of("/foo?flag")).toDetail())
        assertThat(
            view.tx.queryParams, equalTo(
                listOf(HeaderEntry("flag", ""))
            )
        )
    }

    @Test
    fun `detail view pretty-prints JSON request body`(approver: Approver) {
        val view = TransactionDetailView(
            tx(
                request = Request(GET, "/foo")
                    .with(CONTENT_TYPE of APPLICATION_JSON)
                    .body("""{"a":1,"b":"hello"}""")
            ).toDetail()
        )
        approver.assertApproved(view.tx.requestBody, APPLICATION_JSON)
    }

    @Test
    fun `detail view pretty-prints JSON response body`(approver: Approver) {
        val view = TransactionDetailView(
            tx(
                response = Response(OK)
                    .with(CONTENT_TYPE of APPLICATION_JSON)
                    .body("""{"x":[1,2]}""")
            ).toDetail()
        )
        approver.assertApproved(view.tx.responseBody, APPLICATION_JSON)
    }

    @Test
    fun `detail view leaves non-JSON body as-is`() {
        val view = TransactionDetailView(
            tx(
                request = Request(GET, "/foo")
                    .with(CONTENT_TYPE of TEXT_PLAIN)
                    .body("plain text body")
            ).toDetail()
        )
        assertThat(view.tx.requestBody, equalTo("plain text body"))
    }

    @Test
    fun `detail view handles malformed JSON gracefully`() {
        val view = TransactionDetailView(
            tx(
                request = Request(GET, "/foo")
                    .with(CONTENT_TYPE of APPLICATION_JSON)
                    .body("not json")
            ).toDetail()
        )
        assertThat(view.tx.requestBody, equalTo("not json"))
    }

    @Test
    fun `row view detects chaos from response header`() {
        val view = TransactionRowView(
            tx(response = Response(OK).header("x-http4k-chaos", "Latency (100ms)")).toSummary()
        )
        assertThat(view.tx.isChaos, equalTo(true))
        assertThat(view.tx.chaosInfo, equalTo("Latency (100ms)"))
    }

    @Test
    fun `row view has no chaos info without header`() {
        val view = TransactionRowView(tx().toSummary())
        assertThat(view.tx.isChaos, equalTo(false))
    }
}
