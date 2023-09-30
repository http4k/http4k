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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.openqa.selenium.By

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
                .parse("""<body><$tagName>none</$tagName></body>""")
                .outputSettings(Document.OutputSettings().prettyPrint(false))

        val bodyElement = document.getElementsByTag("body").first()!!
        val tagElement = document.getElementsByTag(tagName).first()!!

        tagElement.attr(htmxVerb, htmxUri)
        if (htmxSwap != null) {
            tagElement.attr("hx-swap", htmxSwap)
        }

        return HtmxJsoupWebElement(JSoupWebElement(navigate, getURL, bodyElement), handler)
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

        val body = createTestElement("div", htmxVerb, htmxUri, htmxSwap, handler)

        body.findElement(By.tagName("div"))!!.click()

        body.delegate.element.getElementsByTag("div").first()?.clearAttributes()

        assertThat(body.toString(), equalTo("<body>$expectedResponse</body>"))
    }

    @Nested
    inner class `handles hx-methods` {
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

    @Nested
    inner class `Handles different hx-swap` {
        @Test
        fun `issues a GET request on click with outerHTML swap`() {
            asssertClickResponds(
                htmxVerb = "hx-get",
                htmxUri = "/test",
                htmxSwap = "outerHTML",
                expectedMethod = Method.GET,
                expectedResponse = "responded"
            )
        }

        @Test
        fun `issues a GET request on click with beforebegin swap`() {
            asssertClickResponds(
                htmxVerb = "hx-get",
                htmxUri = "/test",
                htmxSwap = "beforebegin",
                expectedMethod = Method.GET,
                expectedResponse = "responded<div>none</div>"
            )
        }

        @Test
        fun `issues a GET request on click with afterbegin swap`() {
            asssertClickResponds(
                htmxVerb = "hx-get",
                htmxUri = "/test",
                htmxSwap = "afterbegin",
                expectedMethod = Method.GET,
                expectedResponse = "<div>respondednone</div>"
            )
        }

        @Test
        fun `issues a GET request on click with beforeend swap`() {
            asssertClickResponds(
                htmxVerb = "hx-get",
                htmxUri = "/test",
                htmxSwap = "beforeend",
                expectedMethod = Method.GET,
                expectedResponse = "<div>noneresponded</div>"
            )
        }

        @Test
        fun `issues a GET request on click with afterend swap`() {
            asssertClickResponds(
                htmxVerb = "hx-get",
                htmxUri = "/test",
                htmxSwap = "afterend",
                expectedMethod = Method.GET,
                expectedResponse = "<div>none</div>responded"
            )
        }

        @Test
        fun `issues a GET request on click with delete swap`() {
            asssertClickResponds(
                htmxVerb = "hx-get",
                htmxUri = "/test",
                htmxSwap = "delete",
                expectedMethod = Method.GET,
                expectedResponse = ""
            )
        }

        @Test
        fun `issues a GET request on click with none swap`() {
            asssertClickResponds(
                htmxVerb = "hx-get",
                htmxUri = "/test",
                htmxSwap = "none",
                expectedMethod = Method.GET,
                expectedResponse = "<div>none</div>"
            )
        }
    }

}
