package org.http4k.routing

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.anything
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.*
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasContentType
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.Test
import java.io.ByteArrayInputStream


class NewResourceLoadingHandlerTest {

    private val resources = HashMap<String, Resource>()
    private val handler = NewResourceLoadingHandler("/root", InMemoryResourceLoader(resources), emptyMap())

    @Test fun `no resource returns NOT_FOUND`() {
        assertThat(handler(Request(Method.GET, Uri.of("/root/nosuch"))), equalTo(Response(Status.NOT_FOUND)))
    }

    @Test fun `response has content type, length and body`() {
        resources["file.html"] = StringResource("content")
        assertThat(
            handler(Request(Method.GET, Uri.of("/root/file.html"))),
            allOf(
                hasStatus(Status.OK),
                hasContentType(ContentType.TEXT_HTML.withNoDirective()),
                hasHeader("Content-Length", "7"),
                hasBody("content")
            ))
    }

    @Test fun `response has no length if resource doesn't`() {
        resources["file.html"] = StringResource("content", length = null)
        assertThat(
            handler(Request(Method.GET, Uri.of("/root/file.html"))),
            allOf(
                hasStatus(Status.OK),
                hasHeader("Content-Length", null)
            ))
    }
}

data class StringResource(
    val s: String,
    override val length: Long? = s.length.toLong()
) : Resource {
    override fun toStream() = ByteArrayInputStream(s.toByteArray())
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