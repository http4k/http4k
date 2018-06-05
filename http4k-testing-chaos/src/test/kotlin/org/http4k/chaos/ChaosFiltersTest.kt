package org.http4k.chaos

import org.http4k.core.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChaosFiltersTest {
    @Test
    fun `filter with request injection policy should apply behaviour on request`() {
        val req = Request(Method.GET, "")
        val resp = Response(Status.OK)
        val injectedResponse = ChaosFilter(
            object : ChaosPolicy {
                override fun shouldInject(request: Request) = true
            },
            object : ChaosBehaviour {
                override fun inject(request: Request): Request {
                    assertEquals(req, request)
                    return request
                }
            }
        ).then({ _ -> resp })(req)
        assertTrue(injectedResponse.header(CHAOS_HEADER) != null)
    }

    @Test
    fun `filter with response injection policy should apply behaviour on response`() {
        val req = Request(Method.GET, "")
        val resp = Response(Status.OK)
        val injectedResponse = ChaosFilter(
            object : ChaosPolicy {
                override fun shouldInject(response: Response) = true
            },
            object : ChaosBehaviour {
                override fun inject(response: Response): Response {
                    assertEquals(resp, response)
                    return response
                }
            }
        ).then({ _ -> resp })(req)
        assertTrue(injectedResponse.header(CHAOS_HEADER) != null)
    }
}
