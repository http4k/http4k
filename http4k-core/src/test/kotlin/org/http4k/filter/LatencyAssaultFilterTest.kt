package org.http4k.filter

import junit.framework.TestCase.assertTrue
import org.http4k.core.*
import org.http4k.toHttpHandler
import org.junit.Test

class LatencyAssaultFilterTest {
    @Test
    fun `latency injected`() {
        val req = Request(Method.GET, "")
        val resp = Response(Status.OK)
        val injectedResponse = ChaosFilters.LatencyInjectionFilter(100, 1000, 100).then(resp.toHttpHandler())(req)
        assertTrue(injectedResponse.header(LATENCY_HEADER) != null)
    }

    @Test
    fun `latency not injected`() {
        val req = Request(Method.GET, "")
        val resp = Response(Status.OK)
        val injectedResponse = ChaosFilters.LatencyInjectionFilter(100, 1000, 0).then(resp.toHttpHandler())(req)
        assertTrue(injectedResponse.header(LATENCY_HEADER) == null)
    }
}
