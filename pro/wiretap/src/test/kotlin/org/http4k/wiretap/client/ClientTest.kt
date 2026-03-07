package org.http4k.wiretap.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ResponseFilters
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.wiretap.domain.Direction
import org.http4k.wiretap.domain.Direction.*
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.util.Json.json
import org.http4k.wiretap.util.Templates
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Clock
import java.time.Duration
import java.time.Instant.EPOCH
import java.time.ZoneOffset.UTC

@ExtendWith(ApprovalTest::class)
class ClientTest {

    private val clock = Clock.fixed(EPOCH, UTC)
    private val templates: TemplateRenderer = Templates()
    private val renderer = DatastarElementRenderer(templates)
    private val transactions = TransactionStore.InMemory()

    private val client = InboundClient(
        clock = clock, transactions,
    ) { Response(OK).body("proxied") }.http(renderer, templates)

    private val outbound = OutboundClient(
        httpClient = { Response(OK).body("direct") }, clock = clock,
        transactions,
    ).http(renderer, templates)

    @Test
    fun `inbound client index page returns OK`(approver: Approver) {
        approver.assertApproved(client(Request(GET, "/inbound")))
    }

    @Test
    fun `outbound client index page returns OK`(approver: Approver) {
        approver.assertApproved(outbound(Request(GET, "/outbound")))
    }

    @Test
    fun `inbound client import pre-populates from transaction`(approver: Approver) {
        transactions.record(
            HttpTransaction(
                request = Request(POST, Uri.of("http://example.com/api"))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .body("""{"key":"value"}"""),
                response = Response(OK),
                duration = Duration.ofMillis(100),
                start = EPOCH
            ),
            Inbound
        )

        val txId = transactions.list().first().id
        approver.assertApproved(client(Request(GET, "/inbound").query("import", txId.toString())))
    }

    @Test
    fun `outbound client records transactions through decorated handler`() {
        val recordingFilter = ResponseFilters.ReportHttpTransaction(clock) { tx ->
            transactions.record(tx, Outbound)
        }

        val recordingOutbound = OutboundClient(
            httpClient = recordingFilter.then { Response(OK).body("recorded") }, clock = clock,
            transactions,
        ).http(renderer, templates)

        val response = recordingOutbound(
            Request(POST, "/outbound/send").json(
                ClientRequest(
                    method = GET,
                    url = Uri.of("http://example.com/test")
                )
            )
        )

        assertThat(response.status, equalTo(OK))
        assertThat(transactions.list().size, equalTo(1))
        assertThat(transactions.list().first().direction, equalTo(Outbound))
    }

    @Test
    fun `outbound client import pre-populates from outbound transaction`(approver: Approver) {
        val tx = transactions.record(
            HttpTransaction(
                request = Request(GET, Uri.of("https://external.com/data")),
                response = Response(OK),
                duration = Duration.ofMillis(50),
                start = EPOCH
            ),
            Outbound
        )

        approver.assertApproved(outbound(Request(GET, "/outbound").query("import", tx.id.toString())))
    }
}
