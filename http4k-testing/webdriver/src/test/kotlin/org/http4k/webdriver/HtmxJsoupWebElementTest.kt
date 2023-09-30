package org.http4k.webdriver

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.jupiter.api.Test

class HtmxJsoupWebElementTest {

    private var newLocation: Pair<Method, String>? = null
    private val navigate: (Request) -> Unit = { it -> newLocation = it.method to it.uri.toString() }
    private val getURL: () -> String? = { null }

    private fun createTestElement(
        tagName: String,
        htmxVerb: String,
        htmxUri: String,
        htmxSwap: String?,
        handler: HttpHandler
    ): HtmxJsoupWebElement {
        val document =
            Jsoup
                .parse("""<$tagName>no response</$tagName>""")
                .outputSettings(Document.OutputSettings().prettyPrint(false))

        val element =
            JSoupWebElement(navigate, getURL, document.getElementsByTag(tagName).first()!!)

        element.element.attr(htmxVerb, htmxUri)
        if (htmxSwap != null) {
            element.element.attr("hx-swap", htmxSwap)
        }

        return HtmxJsoupWebElement(element, handler)
    }

    private fun asssertClickResponds(
        htmxVerb: String,
        htmxUri: String,
        htmxSwap: String?,
        expectedMethod: Method,
        expectedResponse: String
    ) {
        val handler: HttpHandler = { req ->
            if (req.method == expectedMethod && req.uri.path == "/test") {
                Response(Status.OK).body("responded")
            } else {
                Response(Status.BAD_REQUEST).body("expected $expectedMethod '/test' but got ${req.method} '${req.uri.path}'")
            }
        }
        val div = createTestElement("div", htmxVerb, htmxUri, htmxSwap, handler)
        div.click()

        assertThat(div.delegate.element.clearAttributes().toString(), equalTo(expectedResponse))

    }

    @Test
    fun `issues a GET request on click with default swap`() {
        asssertClickResponds(
            htmxVerb = "hx-get",
            htmxUri = "/test",
            htmxSwap = null,
            expectedMethod = Method.GET,
            expectedResponse = "<div>responded</div>"
        )
    }

    @Test
    fun `issues a GET request on click with innerHtml swap`() {
        asssertClickResponds(
            htmxVerb = "hx-get",
            htmxUri = "/test",
            htmxSwap = "innerHTML",
            expectedMethod = Method.GET,
            expectedResponse = "<div>responded</div>"
        )
    }

    @Test
    fun `issues a data-GET request on click with innerHtml swap`() {
        asssertClickResponds(
            htmxVerb = "data-hx-get",
            htmxUri = "/test",
            htmxSwap = "innerHTML",
            expectedMethod = Method.GET,
            expectedResponse = "<div>responded</div>"
        )
    }

    @Test
    fun `issues a POST request on click with default swap`() {
        asssertClickResponds(
            htmxVerb = "hx-post",
            htmxUri = "/test",
            htmxSwap = null,
            expectedMethod = Method.POST,
            expectedResponse = "<div>responded</div>"
        )
    }

    @Test
    fun `issues a DELETE request on click with default swap`() {
        asssertClickResponds(
            htmxVerb = "hx-delete",
            htmxUri = "/test",
            htmxSwap = null,
            expectedMethod = Method.DELETE,
            expectedResponse = "<div>responded</div>"
        )
    }

    @Test
    fun `issues a PATCH request on click with default swap`() {
        asssertClickResponds(
            htmxVerb = "hx-patch",
            htmxUri = "/test",
            htmxSwap = null,
            expectedMethod = Method.PATCH,
            expectedResponse = "<div>responded</div>"
        )
    }

    @Test
    fun `issues a PUT request on click with default swap`() {
        asssertClickResponds(
            htmxVerb = "hx-put",
            htmxUri = "/test",
            htmxSwap = null,
            expectedMethod = Method.PUT,
            expectedResponse = "<div>responded</div>"
        )
    }

}
