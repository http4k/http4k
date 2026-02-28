package org.http4k.wiretap.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
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
import org.http4k.wiretap.domain.Direction
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.util.Json
import org.http4k.wiretap.util.Templates
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant

class ClientTest {

    private val templates: TemplateRenderer = Templates()
    private val renderer = DatastarElementRenderer(templates)
    private val transactions = TransactionStore.InMemory()

    private val client = InboundClient(
        clock = Clock.systemUTC(), transactions,
        templates
    ) { Response(OK).body("proxied") }.http(renderer)

    private val outbound = OutboundClient(
        httpClient = { Response(OK).body("direct") }, clock = Clock.systemUTC(),
        transactions,
        templates
    ).http(renderer)

    @Test
    fun `inbound client index page returns OK with correct basePath and title`() {
        val response = client(Request(GET, "/client"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), containsSubstring("/__wiretap/client/"))
        assertThat(response.bodyString(), containsSubstring("Inbound Client"))
    }

    @Test
    fun `outbound client index page returns OK with correct basePath and title`() {
        val response = outbound(Request(GET, "/outbound"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), containsSubstring("/__wiretap/outbound/"))
        assertThat(response.bodyString(), containsSubstring("Outbound Client"))
    }

    @Test
    fun `inbound client import pre-populates from transaction`() {
        transactions.record(
            HttpTransaction(
                request = Request(POST, Uri.of("http://example.com/api"))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .body("""{"key":"value"}"""),
                response = Response(OK),
                duration = Duration.ofMillis(100),
                start = Instant.now()
            ),
            Direction.Inbound
        )

        val txId = transactions.list().first().id
        val response = client(Request(GET, "/client?import=$txId"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), containsSubstring("POST"))
        assertThat(response.bodyString(), containsSubstring("http://example.com/api"))
        assertThat(response.bodyString(), containsSubstring("application/json"))
    }

    @Test
    fun `outbound client records transactions through decorated handler`() {
        val clock = Clock.systemUTC()

        val recordingFilter = ResponseFilters.ReportHttpTransaction(clock) { tx ->
            transactions.record(tx, Direction.Outbound)
        }

        val recordingOutbound = OutboundClient(
            httpClient = recordingFilter.then { Response(OK).body("recorded") }, clock = clock,
            transactions,
            templates
        ).http(renderer)

        val body = Json.asFormatString(ClientRequest(method = GET, url = Uri.of("http://example.com/test")))
        val response = recordingOutbound(Request(POST, "/outbound/send").body(body))

        assertThat(response.status, equalTo(OK))
        assertThat(transactions.list().size, equalTo(1))
        assertThat(transactions.list().first().direction, equalTo(Direction.Outbound))
    }

    @Test
    fun `outbound client import pre-populates from outbound transaction`() {
        val tx = transactions.record(
            HttpTransaction(
                request = Request(GET, Uri.of("https://external.com/data")),
                response = Response(OK),
                duration = Duration.ofMillis(50),
                start = Instant.now()
            ),
            Direction.Outbound
        )

        val response = outbound(Request(GET, "/outbound?import=${tx.id}"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), containsSubstring("https://external.com/data"))
    }
}
