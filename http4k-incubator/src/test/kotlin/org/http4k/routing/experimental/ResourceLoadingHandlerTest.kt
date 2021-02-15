package org.http4k.routing.experimental

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.anything
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.apache.hc.core5.http.impl.io.EmptyInputStream
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.MemoryRequest
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.NOT_MODIFIED
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.etag.ETag
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasContentType
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.Router
import org.http4k.routing.RouterMatch
import org.junit.jupiter.api.Test
import java.time.Instant

class NewResourceLoadingHandlerTest {

    private val resources = HashMap<String, Resource>()
    private val handler = ResourceLoadingHandler("/root", InMemoryResourceLoader(resources))
    private val now = Instant.parse("2018-08-09T23:06:00Z")

    @Test
    fun `no resource returns NOT_FOUND`() {
        assertThat(handler(MemoryRequest(GET, Uri.of("/root/nosuch"))), equalTo(Response(NOT_FOUND)))
    }

    @Test
    fun `returns content, content type, length and body`() {
        resources["/file.txt"] = InMemoryResource("content", TEXT_PLAIN, lastModified = now, etag = ETag("etag-value", weak = true))
        assertThat(
            handler(Request(GET, Uri.of("/root/file.txt"))),
            allOf(
                hasStatus(OK),
                hasContentType(TEXT_PLAIN.withNoDirectives()),
                hasHeader("Content-Length", "7"),
                hasHeader("Last-Modified", "Thu, 9 Aug 2018 23:06:00 GMT"),
                hasHeader("ETag", """W/"etag-value""""),
                hasBody("content")
            ))
    }

    @Test
    fun `returns no length and last modified if null from resource`() {
        resources["/file.txt"] = IndeterminateLengthResource()
        assertThat(
            handler(Request(GET, Uri.of("/root/file.txt"))),
            allOf(
                hasStatus(OK),
                hasHeader("Content-Length", absent()),
                hasHeader("Last-Modified", absent()),
                hasHeader("ETag", absent())
            ))
    }

    @Test
    fun `returns content if resource is modified by time`() {
        resources["/file.txt"] = InMemoryResource("content", TEXT_PLAIN, lastModified = now)
        assertThat(
            handler(MemoryRequest(GET, Uri.of("/root/file.txt"),
                listOf("If-Modified-Since" to "Thu, 9 Aug 2018 23:05:59 GMT"))),
            allOf(
                hasStatus(OK),
                hasHeader("Last-Modified", "Thu, 9 Aug 2018 23:06:00 GMT"),
                hasBody("content")
            ))
    }

    @Test
    fun `returns NOT_MODIFIED if resource is not modified by time`() {
        resources["/file.txt"] = InMemoryResource("content", TEXT_PLAIN, lastModified = now)
        assertThat(
            handler(MemoryRequest(GET, Uri.of("/root/file.txt"),
                listOf("If-Modified-Since" to "Thu, 9 Aug 2018 23:06:00 GMT"))),
            allOf(
                hasStatus(NOT_MODIFIED),
                hasHeader("Last-Modified", "Thu, 9 Aug 2018 23:06:00 GMT"),
                hasBody("")
            ))
        assertThat(
            handler(MemoryRequest(GET, Uri.of("/root/file.txt"),
                listOf("If-Modified-Since" to "Thu, 9 Aug 2018 23:06:01 GMT"))),
            allOf(
                hasStatus(NOT_MODIFIED),
                hasHeader("Last-Modified", "Thu, 9 Aug 2018 23:06:00 GMT"),
                hasBody("")
            ))
    }

    @Test
    fun `returns content if no last modified property`() {
        resources["/file.txt"] = InMemoryResource("content", TEXT_PLAIN, lastModified = null)
        assertThat(
            handler(MemoryRequest(GET, Uri.of("/root/file.txt"),
                listOf("If-Modified-Since" to "Thu, 9 Aug 2018 23:05:59 GMT"))),
            allOf(
                hasStatus(OK),
                hasHeader("Last-Modified", absent()),
                hasBody("content")
            ))
    }

    @Test
    fun `returns content for incorrect date format`() {
        resources["/file.txt"] = InMemoryResource("content", TEXT_PLAIN)
        assertThat(
            handler(MemoryRequest(GET, Uri.of("/root/file.txt"),
                listOf("If-Modified-Since" to "NOT A DATE"))),
            allOf(
                hasStatus(OK),
                hasBody("content")
            ))
    }

    @Test
    fun `returns content if resource does not match etag`() {
        resources["/file.txt"] = InMemoryResource("content", TEXT_PLAIN, etag = ETag("etag-value", weak = true))
        assertThat(
            handler(MemoryRequest(GET, Uri.of("/root/file.txt"),
                listOf("If-None-Match" to """"something-else""""))),
            allOf(
                hasStatus(OK),
                hasHeader("ETag", """W/"etag-value""""),
                hasBody("content")
            ))
    }

    @Test
    fun `returns NOT_MODIFIED if resource does match etag`() {
        resources["/file.txt"] = InMemoryResource("content", TEXT_PLAIN, etag = ETag("etag-value", weak = true))
        assertThat(
            handler(MemoryRequest(GET, Uri.of("/root/file.txt"),
                listOf("If-None-Match" to """"something-else", W/"etag-value""""))),
            allOf(
                hasStatus(NOT_MODIFIED),
                hasHeader("ETag", """W/"etag-value""""),
                hasBody("")
            ))
        assertThat(
            handler(MemoryRequest(GET, Uri.of("/root/file.txt"),
                listOf("If-None-Match" to """*"""))),
            allOf(
                hasStatus(NOT_MODIFIED),
                hasHeader("ETag", """W/"etag-value""""),
                hasBody("")
            ))
        assertThat( // should match strong etag even though resource is weak
            handler(MemoryRequest(GET, Uri.of("/root/file.txt"),
                listOf("If-None-Match" to """"etag-value""""))),
            allOf(
                hasStatus(NOT_MODIFIED),
                hasHeader("ETag", """W/"etag-value""""),
                hasBody("")
            ))
    }

    @Test
    fun `returns content if no etag property`() {
        resources["/file.txt"] = InMemoryResource("content", TEXT_PLAIN, etag = null)
        assertThat(
            handler(MemoryRequest(GET, Uri.of("/root/file.txt"),
                listOf("If-None-Match" to """*"""))),
            allOf(
                hasStatus(OK),
                hasHeader("ETag", absent()),
                hasBody("content")
            ))
    }
}

private class IndeterminateLengthResource : Resource {
    override fun openStream() = EmptyInputStream.INSTANCE!!
}

private class InMemoryResourceLoader(val resources: Map<String, Resource>) : Router {
    override fun match(request: Request): RouterMatch = resources[request.uri.path]?.let {
        RouterMatch.MatchingHandler(it, description)
    } ?: RouterMatch.Unmatched(description)
}

/**
 * Returns a matcher that matches if all of the supplied matchers match.
 */
fun <T> allOf(matchers: List<Matcher<T>>): Matcher<T> = matchers.reducedWith(Matcher<T>::and)

/**
 * Returns a matcher that matches if all of the supplied matchers match.
 */
fun <T> allOf(vararg matchers: Matcher<T>): Matcher<T> = allOf(matchers.asList())

@Suppress("UNCHECKED_CAST")
private fun <T> List<Matcher<T>>.reducedWith(op: (Matcher<T>, Matcher<T>) -> Matcher<T>): Matcher<T> = when {
    isEmpty() -> anything
    else -> reduce(op)
}
