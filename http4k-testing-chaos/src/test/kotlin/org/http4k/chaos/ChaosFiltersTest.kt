package org.http4k.chaos

import org.http4k.core.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChaosFiltersTest {
    @Test
    fun `pre filter should apply behaviour on request`() {
        val request = Request(Method.GET, "")
        val response = Response(Status.OK)
        val injectedResponse = ChaosFilters.ChaosPreFilter(
            AlwaysInjectChaosPolicy(),
            object : ChaosBehaviour {
                override fun inject(message: HttpMessage) {
                    assertEquals(message, request)
                }
            }
        ).then({ _ -> response })(request)
        assertTrue(injectedResponse.header(LATENCY_HEADER) != null)
    }

    @Test
    fun `post filter should apply behaviour on response`() {
        val request = Request(Method.GET, "")
        val response = Response(Status.OK)
        val injectedResponse = ChaosFilters.ChaosPostFilter(
            AlwaysInjectChaosPolicy(),
            object : ChaosBehaviour {
                override fun inject(message: HttpMessage) {
                    assertEquals(message, response)
                }
            }
        ).then({ _ -> response })(request)
        assertTrue(injectedResponse.header(LATENCY_HEADER) != null)
    }
}
