package org.http4k

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.present
import org.http4k.TracingFunctionalTest.Service.ServiceA
import org.http4k.TracingFunctionalTest.Service.ServiceB
import org.http4k.TracingFunctionalTest.Service.ServiceC
import org.http4k.client.JavaHttpClient
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.ServerFilters
import org.http4k.filter.ZipkinTraces
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.util.concurrent.ConcurrentHashMap

class TracingFunctionalTest {

    enum class Service { ServiceA, ServiceB, ServiceC }

    private val traces = ConcurrentHashMap<Service, ZipkinTraces>()
    private val registry = ConcurrentHashMap<Service, Int>()

    private fun Service.start(vararg clients: HttpHandler) =
        ServerFilters.RequestTracing(recordTraces())
            .then(makeCalls(*clients))
            .then { Response(Status.OK) }
            .asServer(SunHttp(0)).start().apply { registry[this@start] = port() }

    private fun clientFor(service: Service) =
        ClientFilters.RequestTracing()
            .then(ClientFilters.SetBaseUriFrom(Uri.of("http://localhost:${service.port()}")))
            .then(JavaHttpClient())

    private fun makeCalls(vararg clients: HttpHandler) = Filter { next ->
        {
            clients.forEach { client -> client(Request(GET, "")) }
            next(it)
        }
    }

    private fun Service.recordTraces() = { _: Request, trace: ZipkinTraces -> traces.put(this, trace) }
    private fun Service.traces() = traces[this] ?: fail("trace not present")
    private fun Service.port() = registry[this] ?: error("could not find server port")

    @Test
    fun `single server call`() {
        ServiceA.start()
        val clientA = clientFor(ServiceA)

        clientA(Request(GET, "/"))

        val traces = ServiceA.traces()
        assertThat(traces.traceId, present())
        assertThat(traces.spanId, present())
        assertThat(traces.parentSpanId, present())
    }

    @Test
    fun `server to server call`() {
        ServiceB.start()
        ServiceA.start(clientFor(ServiceB))
        val clientA = clientFor(ServiceA)

        clientA(Request(GET, "/"))

        val traceA = ServiceA.traces()
        val traceB = ServiceB.traces()

        assertThat(traceA.traceId, equalTo(traceB.traceId))
        assertThat(traceB.parentSpanId, equalTo(traceA.spanId))
        assertThat(traceB.spanId, !equalTo(traceA.spanId))
    }

    @Test
    fun `multiple calls`() {
        ServiceB.start()
        ServiceC.start()
        ServiceA.start(clientFor(ServiceB), clientFor(ServiceC))
        val clientA = clientFor(ServiceA)

        clientA(Request(GET, "/"))

        val traceA = ServiceA.traces()
        val traceB = ServiceB.traces()
        val traceC = ServiceC.traces()

        assertThat(setOf(traceA.traceId, traceB.traceId, traceC.traceId), hasSize(equalTo(1)))
        assertThat(setOf(traceB.parentSpanId, traceC.parentSpanId), equalTo(setOf(traceA.spanId)))
        assertThat(setOf(traceA.spanId, traceB.spanId, traceC.spanId), hasSize(equalTo(3)))
    }
}
