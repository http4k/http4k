package org.http4k.routing

import org.http4k.core.ContentEncodingName.Companion.COMPRESS
import org.http4k.core.ContentEncodingName.Companion.DEFLATE
import org.http4k.core.ContentEncodingName.Companion.GZIP
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Exactly
import org.http4k.core.Method.GET
import org.http4k.core.PriorityList
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Wildcard
import org.http4k.core.q
import org.http4k.core.with
import org.http4k.filter.ServerFilters.SetContentType
import org.http4k.lens.ACCEPT_ENCODING
import org.http4k.lens.ACCEPT_LANGUAGE
import org.http4k.lens.Header
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale.ENGLISH
import java.util.Locale.FRENCH
import java.util.Locale.GERMAN

/**
 * See [org.http4k.routing.ProactiveContentNegotiationRoutingTest] for
 * a detailed test of the underlying algorithm.
 */
class StandardAcceptHeadersRoutingTest {
    @Test
    fun `routes by language content negotiation`() {
        val router = routes(
            "/hello" bind GET to contentLanguages(
                ENGLISH to { Response(OK).body("Hello") },
                FRENCH to { Response(OK).body("Bonjour") },
                GERMAN to { Response(OK).body("Guten Tag") }
            )
        ).withFilter(SetContentType(TEXT_PLAIN))
        
        val rsp = router(
            Request(GET, "/hello").with(
                Header.ACCEPT_LANGUAGE of PriorityList(
                    Exactly(FRENCH) q 1.0,
                    Exactly(GERMAN) q 0.75,
                    Wildcard q 0.25
                )
            )
        )
        
        assertEquals(OK, rsp.status)
        assertEquals("Bonjour", rsp.bodyString())
        assertEquals("fr", rsp.header("content-language"))
        assertEquals("accept-language", rsp.header("vary")?.lowercase())
    }
    
    @Test
    fun `routes by encoding content negotiation`() {
        val router = routes(
            "/hello" bind GET to contentEncodings(
                GZIP to { Response(OK).body("gzipped") },
                DEFLATE to { Response(OK).body("deflated") },
                COMPRESS to { Response(OK).body("compressed") }
            )
        ).withFilter(SetContentType(TEXT_PLAIN))
        
        val rsp = router(
            Request(GET, "/hello").with(
                Header.ACCEPT_ENCODING of PriorityList(
                    Exactly(DEFLATE) q 1.0,
                    Exactly(COMPRESS) q 0.75,
                    Wildcard q 0.25
                )
            )
        )
        
        assertEquals(OK, rsp.status)
        assertEquals("deflated", rsp.bodyString())
        assertEquals("deflate", rsp.header("content-encoding"))
        assertEquals("accept-encoding", rsp.header("vary")?.lowercase())
    }
}
