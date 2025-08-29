package org.http4k.routing

import org.http4k.core.Exactly
import org.http4k.core.Method.GET
import org.http4k.core.PriorityList
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_ACCEPTABLE
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Wildcard
import org.http4k.core.fromSimpleRangeHeader
import org.http4k.core.q
import org.http4k.core.toSimpleRangeHeader
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.routing.ProactiveContentNegotiationRoutingTest.ExampleOption.a
import org.http4k.routing.ProactiveContentNegotiationRoutingTest.ExampleOption.x
import org.http4k.routing.ProactiveContentNegotiationRoutingTest.ExampleOption.y
import org.http4k.routing.ProactiveContentNegotiationRoutingTest.ExampleOption.z
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProactiveContentNegotiationRoutingTest {
    enum class ExampleOption { x, y, z, a }
    
    val acceptHeader = Header.map(
        { PriorityList.fromSimpleRangeHeader(it, ExampleOption::valueOf) },
        { it.toSimpleRangeHeader { it.name } }
    ).optional("accept-option")
    
    val reportHeader = Header.map(ExampleOption::valueOf, { it.name })
        .optional("example-option")
    
    val router = routes(
        "/hello" bind GET to proactiveContentNegotiation(
            acceptBy = acceptHeader,
            reportBy = reportHeader,
            match = { r, o -> r.matches(o) },
            listOf(
                x to { Response(OK).body("x response") },
                y to { Response(OK).body("y response") },
                z to { Response(OK).body("z response") }
            )
        )
    )
    
    @Test
    fun `routes to explicit match`() {
        val rsp = router(
            Request(GET, "/hello").with(
                acceptHeader of PriorityList(
                    Exactly(y) q 0.75,
                    Wildcard q 0.25
                )
            )
        )
        
        assertEquals(OK, rsp.status)
        assertEquals("y response", rsp.bodyString())
        assertEquals("y", rsp.header("example-option"))
        assertEquals("accept-option", rsp.header("vary")?.lowercase())
    }
    
    @Test
    fun `routes wildcard match to first offered option`() {
        val rsp = router(
            Request(GET, "/hello").with(
                acceptHeader of PriorityList(
                    Exactly(a) q 0.75,
                    Wildcard q 0.25
                )
            )
        )
        
        assertEquals(OK, rsp.status)
        assertEquals("x response", rsp.bodyString())
        assertEquals("x", rsp.header("example-option"))
        assertEquals("accept-option", rsp.header("vary")?.lowercase())
    }
    
    @Test
    fun `reports if no acceptable option is found`() {
        val rsp = router(
            Request(GET, "/hello").with(
                acceptHeader of PriorityList(
                    Exactly(a) q 1.0
                )
            )
        )
        
        assertEquals(NOT_ACCEPTABLE, rsp.status)
        assertEquals("accept-option", rsp.header("vary")?.lowercase())
    }
    
    @Test
    fun `routes to the first option if request does not specify preference`() {
        val rsp = router(
            Request(GET, "/hello")
        )
        
        assertEquals(OK, rsp.status)
        assertEquals("x response", rsp.bodyString())
        assertEquals("x", rsp.header("example-option"))
        assertEquals("accept-option", rsp.header("vary")?.lowercase())
    }
}
