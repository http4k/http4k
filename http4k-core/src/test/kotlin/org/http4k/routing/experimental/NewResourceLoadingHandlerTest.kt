package org.http4k.routing.experimental

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.anything
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.apache.http.impl.io.EmptyInputStream
import org.http4k.core.*
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasContentType
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.Test
import java.time.Instant


class NewResourceLoadingHandlerTest {

    private val mimeTypes = MimeTypes()
    private val resources = HashMap<String, Resource>()
    private val handler = NewResourceLoadingHandler("/root", InMemoryResourceLoader(resources), emptyMap())
    private val now = Instant.parse("2018-08-09T23:06:00Z")

    @Test fun `no resource returns NOT_FOUND`() {
        assertThat(handler(MemoryRequest(Method.GET, Uri.of("/root/nosuch"))), equalTo(Response(Status.NOT_FOUND)))
    }

    @Test fun `returns content, content type, length and body`() {
        resources["file.html"] = InMemoryResource("content", lastModified = now)
        assertThat(
            handler(MemoryRequest(Method.GET, Uri.of("/root/file.html"))),
            allOf(
                hasStatus(Status.OK),
                hasContentType(mimeTypes.forFile("file.html")),
                hasHeader("Content-Length", "7"),
                hasHeader("Last-Modified", "Thu, 9 Aug 2018 23:06:00 GMT"),
                hasBody("content")
            ))
    }

    @Test fun `returns no length and last modified if resource doesn't`() {
        resources["file.html"] = IndeterminateLengthResource()
        assertThat(
            handler(MemoryRequest(Method.GET, Uri.of("/root/file.html"))),
            allOf(
                hasStatus(Status.OK),
                hasHeader("Content-Length", null)
            ))
    }

    @Test fun `returns content if resource is modified`() {
        resources["file.html"] = InMemoryResource("content", lastModified = now)
        assertThat(
            handler(MemoryRequest(Method.GET, Uri.of("/root/file.html"),
                listOf("If-Modified-Since" to "Thu, 9 Aug 2018 23:05:59 GMT"))),
            allOf(
                hasStatus(Status.OK),
                hasHeader("Last-Modified", "Thu, 9 Aug 2018 23:06:00 GMT"),
                hasBody("content")
            ))
    }

    @Test fun `returns NOT_MODIFIED if resource is not modified`() {
        resources["file.html"] = InMemoryResource("content", lastModified = now)
        assertThat(
            handler(MemoryRequest(Method.GET, Uri.of("/root/file.html"),
                listOf("If-Modified-Since" to "Thu, 9 Aug 2018 23:06:00 GMT"))),
            allOf(
                hasStatus(Status.NOT_MODIFIED),
                hasHeader("Last-Modified", "Thu, 9 Aug 2018 23:06:00 GMT"),
                hasBody("")
            ))
        assertThat(
            handler(MemoryRequest(Method.GET, Uri.of("/root/file.html"),
                listOf("If-Modified-Since" to "Thu, 9 Aug 2018 23:06:01 GMT"))),
            allOf(
                hasStatus(Status.NOT_MODIFIED),
                hasHeader("Last-Modified", "Thu, 9 Aug 2018 23:06:00 GMT"),
                hasBody("")
            ))
    }

    @Test fun `returns content if no last modified property`() {
        resources["file.html"] = InMemoryResource("content", lastModified = null)
        assertThat(
            handler(MemoryRequest(Method.GET, Uri.of("/root/file.html"),
                listOf("If-Modified-Since" to "Thu, 9 Aug 2018 23:05:59 GMT"))),
            allOf(
                hasStatus(Status.OK),
                hasHeader("Last-Modified", null),
                hasBody("content")
            ))
    }

    @Test fun `returns content for incorrect date format`() {
        resources["file.html"] = InMemoryResource("content")
        assertThat(
            handler(MemoryRequest(Method.GET, Uri.of("/root/file.html"),
                listOf("If-Modified-Since" to "NOT A DATE"))),
            allOf(
                hasStatus(Status.OK),
                hasBody("content")
            ))
    }
}

private class IndeterminateLengthResource : Resource {
    override fun openStream() = EmptyInputStream.INSTANCE!!
}

class InMemoryResourceLoader(val resources: Map<String, Resource>) : NewResourceLoader {

    override fun resourceFor(path: String): Resource? = resources[path]

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
    isEmpty() -> anything as Matcher<T>
    else -> reduce(op)
}