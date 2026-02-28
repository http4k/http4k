package org.http4k.wiretap

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import com.natpryce.hamkrest.present
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.OpenTelemetryTracing
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.domain.WiretapTransaction
import org.junit.jupiter.api.Test

class HttpWiretapIntegrationTest {

    fun traceparent(tx: WiretapTransaction): String? =
        (tx.transaction.request.header("traceparent") ?: tx.transaction.response.header("traceparent"))
            ?.split("-")?.getOrNull(1)

    private val store = TransactionStore.InMemory()

    @Test
    fun `records transactions via real server`() {
        val poly = Wiretap.Http(transactionStore = store) { _, _, _ ->
            routes("/" bind GET to { Response(OK).body("hello") })
        }

        val server = poly.asServer(Jetty(0)).start()
        try {
            val client = JavaHttpClient()
            val response = client(Request(GET, "http://localhost:${server.port()}/"))
            assertThat(response.status, equalTo(OK))
            assertThat(store.list().size, greaterThan(0))
        } finally {
            server.stop()
        }
    }

    @Test
    fun `recorded transactions contain traceparent from OTel tracing`() {
        val poly = Wiretap.Http(transactionStore = store) { http, oTel, _ ->
            ServerFilters.OpenTelemetryTracing(oTel)
                .then(routes("/" bind GET to { Response(OK).body("hello") }))
        }

        val server = poly.asServer(Jetty(0)).start()
        try {
            val client = JavaHttpClient()
            client(Request(GET, "http://localhost:${server.port()}/"))

            val tx = store.list().first()
            assertThat("traceparent should be extractable", traceparent(tx), present())
        } finally {
            server.stop()
        }
    }
}
