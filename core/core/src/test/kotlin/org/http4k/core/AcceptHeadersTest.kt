package learnhttp4kwithtests

import org.http4k.core.ACCEPT_CHARSET
import org.http4k.core.ACCEPT_ENCODING
import org.http4k.core.ACCEPT_LANGUAGE
import org.http4k.core.PriorityList
import org.http4k.core.ContentEncodingName
import org.http4k.core.Exactly
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Wildcard
import org.http4k.core.q
import org.http4k.core.with
import org.http4k.lens.Header
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.charset.Charset
import java.util.Locale

class AcceptHeadersTest {
    @Test
    fun `can set and get the Accept-Language header`() {
        val preferredLanguages = PriorityList(
            Exactly(Locale.forLanguageTag("en-GB")) q 1.0,
            Exactly(Locale.forLanguageTag("en")) q 0.75,
            Wildcard q 0.25
        )
        
        val rq = Request(GET, "/something")
            .with(Header.ACCEPT_LANGUAGE of preferredLanguages)
        assertEquals("en-GB,en;q=0.75,*;q=0.25", rq.header("Accept-Language"))
        
        val parsed = Header.ACCEPT_LANGUAGE(rq)
        assertEquals(preferredLanguages, parsed)
    }
    
    @Test
    fun `can set and get the Accept-Charset header`() {
        val preferredCharsets = PriorityList(
            Exactly(Charset.forName("UTF-8")) q 1.0,
            Exactly(Charset.forName("ISO-8859-1")) q 0.75,
            Exactly(Charset.forName("US-ASCII")) q 0.6,
            Wildcard q 0.25
        )
        
        val rq = Request(GET, "/something")
            .with(Header.ACCEPT_CHARSET of preferredCharsets)
        assertEquals("utf-8,iso-8859-1;q=0.75,us-ascii;q=0.6,*;q=0.25", rq.header("Accept-Charset"))
        
        val parsed = Header.ACCEPT_CHARSET(rq)
        assertEquals(preferredCharsets, parsed)
    }
    
    @Test
    fun `can set and get the Accept-Encoding header`() {
        val preferredEncodings = PriorityList(
            Exactly(ContentEncodingName.GZIP) q 1.0,
            Exactly(ContentEncodingName.DEFLATE) q 0.75,
            Wildcard q 0.5
        )
        
        val rq = Request(GET, "/something")
            .with(Header.ACCEPT_ENCODING of preferredEncodings)
        assertEquals("gzip,deflate;q=0.75,*;q=0.5", rq.header("Accept-Encoding"))
        
        val parsed = Header.ACCEPT_ENCODING(rq)
        assertEquals(preferredEncodings, parsed)
    }
}
