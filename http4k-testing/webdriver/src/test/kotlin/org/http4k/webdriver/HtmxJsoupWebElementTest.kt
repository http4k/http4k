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

    private val jsoupOutputSettings = Document.OutputSettings().prettyPrint(false)

    private fun createTestElement(
        htmxVerb: String,
        htmxSwap: String?,
        handler: HttpHandler
    ): HtmxJsoupWebElement {
        val document =
            Jsoup
                .parse("""<body><div>-NONE-</div></body>""")
                .outputSettings(jsoupOutputSettings)

        val bodyElement = document.getElementsByTag("body").first()!!
        val tagElement = document.getElementsByTag("div").first()!!

        tagElement.attr(htmxVerb, "/test")
        if (htmxSwap != null) {
            tagElement.attr("hx-swap", htmxSwap)
        }

        return HtmxJsoupWebElement(JSoupWebElement(navigate, getURL, bodyElement), handler)
    }

    private fun asssertClickResponds(
        htmxVerb: String,
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

        val body = createTestElement(htmxVerb, htmxSwap, handler)

        body.findElement(By.tagName("div"))!!.click()

        body.delegate.element.getElementsByTag("div").first()?.clearAttributes()

        assertThat(body.toString(), equalTo("<body>$expectedResponse</body>"))
    }

    @Nested
    inner class `Handles hx-methods` {
        @Test
        fun `issues a GET request on click with default swap`() {
            asssertClickResponds(
                htmxVerb = "hx-get",
                htmxSwap = null,
                expectedMethod = Method.GET,
                expectedResponse = "<div>responded</div>"
            )
        }

        @Test
        fun `issues a GET request on click with innerHtml swap`() {
            asssertClickResponds(
                htmxVerb = "hx-get",
                htmxSwap = "innerHTML",
                expectedMethod = Method.GET,
                expectedResponse = "<div>responded</div>"
            )
        }

        @Test
        fun `issues a data-GET request on click with innerHtml swap`() {
            asssertClickResponds(
                htmxVerb = "data-hx-get",
                htmxSwap = "innerHTML",
                expectedMethod = Method.GET,
                expectedResponse = "<div>responded</div>"
            )
        }

        @Test
        fun `issues a POST request on click with default swap`() {
            asssertClickResponds(
                htmxVerb = "hx-post",
                htmxSwap = null,
                expectedMethod = Method.POST,
                expectedResponse = "<div>responded</div>"
            )
        }

        @Test
        fun `issues a DELETE request on click with default swap`() {
            asssertClickResponds(
                htmxVerb = "hx-delete",
                htmxSwap = null,
                expectedMethod = Method.DELETE,
                expectedResponse = "<div>responded</div>"
            )
        }

        @Test
        fun `issues a PATCH request on click with default swap`() {
            asssertClickResponds(
                htmxVerb = "hx-patch",
                htmxSwap = null,
                expectedMethod = Method.PATCH,
                expectedResponse = "<div>responded</div>"
            )
        }

        @Test
        fun `issues a PUT request on click with default swap`() {
            asssertClickResponds(
                htmxVerb = "hx-put",
                htmxSwap = null,
                expectedMethod = Method.PUT,
                expectedResponse = "<div>responded</div>"
            )
        }
    }

    @Nested
    inner class `Handles hx-swap` {
        @Test
        fun `issues a GET request on click with outerHTML swap`() {
            asssertClickResponds(
                htmxVerb = "hx-get",
                htmxSwap = "outerHTML",
                expectedMethod = Method.GET,
                expectedResponse = "responded"
            )
        }

        @Test
        fun `issues a GET request on click with beforebegin swap`() {
            asssertClickResponds(
                htmxVerb = "hx-get",
                htmxSwap = "beforebegin",
                expectedMethod = Method.GET,
                expectedResponse = "responded<div>-NONE-</div>"
            )
        }

        @Test
        fun `issues a GET request on click with afterbegin swap`() {
            asssertClickResponds(
                htmxVerb = "hx-get",
                htmxSwap = "afterbegin",
                expectedMethod = Method.GET,
                expectedResponse = "<div>responded-NONE-</div>"
            )
        }

        @Test
        fun `issues a GET request on click with beforeend swap`() {
            asssertClickResponds(
                htmxVerb = "hx-get",
                htmxSwap = "beforeend",
                expectedMethod = Method.GET,
                expectedResponse = "<div>-NONE-responded</div>"
            )
        }

        @Test
        fun `issues a GET request on click with afterend swap`() {
            asssertClickResponds(
                htmxVerb = "hx-get",
                htmxSwap = "afterend",
                expectedMethod = Method.GET,
                expectedResponse = "<div>-NONE-</div>responded"
            )
        }

        @Test
        fun `issues a GET request on click with delete swap`() {
            asssertClickResponds(
                htmxVerb = "hx-get",
                htmxSwap = "delete",
                expectedMethod = Method.GET,
                expectedResponse = ""
            )
        }

        @Test
        fun `issues a GET request on click with none swap`() {
            asssertClickResponds(
                htmxVerb = "hx-get",
                htmxSwap = "none",
                expectedMethod = Method.GET,
                expectedResponse = "<div>-NONE-</div>"
            )
        }
    }

    @Nested
    inner class `Handles inheritance and targets` {
        private val alwaysRespondHandler: HttpHandler = { Response(Status.OK).body("responded") }

        @Test
        fun `targets a parent with hx-target 'this'`() {
            val html = Jsoup.parse(
                """
                    |<body>
                    |<div hx-target="this">
                    |<div id="actor" hx-get="/test"/>
                    |</div>
                    |</body>
                """.trimMargin()
            ).outputSettings(jsoupOutputSettings)

            val element = HtmxJsoupWebElement(JSoupWebElement(navigate, getURL, html), alwaysRespondHandler)

            val body = element.findElement(By.tagName("body"))

            element.findElement(By.id("actor"))!!.click()

            assertThat(
                body.toString(),
                equalTo(
                    """
                        |<body>
                        |<div hx-target="this">responded</div>
                        |</body>
                    """.trimMargin()
                )
            )
        }

        @Test
        fun `targets a sibling by id`() {
            val html = Jsoup.parse(
                """
                    |<body>
                    |<div id="foo" hx-get="/test" hx-target="#bar">foo</div>
                    |<div id="bar">bar</div>
                    |</body>
                """.trimMargin()
            ).outputSettings(jsoupOutputSettings)

            val element = HtmxJsoupWebElement(JSoupWebElement(navigate, getURL, html), alwaysRespondHandler)

            val body = element.findElement(By.tagName("body"))

            element.findElement(By.id("foo"))!!.click()

            assertThat(
                body.toString(),
                equalTo(
                    """
                        |<body>
                        |<div id="foo" hx-get="/test" hx-target="#bar">foo</div>
                        |<div id="bar">responded</div>
                        |</body>
                    """.trimMargin()
                )
            )
        }

        @Test
        fun `inherits hx-swap`() {
            val html = Jsoup.parse(
                """
                    |<body>
                    |<div hx-swap="outerHTML">
                    |<div id="foo" hx-get="/test">foo</div>
                    |</div>
                    |</body>
                """.trimMargin()
            ).outputSettings(jsoupOutputSettings)

            val element = HtmxJsoupWebElement(JSoupWebElement(navigate, getURL, html), alwaysRespondHandler)

            val body = element.findElement(By.tagName("body"))

            element.findElement(By.id("foo"))!!.click()

            assertThat(
                body.toString(),
                equalTo(
                    """
                        |<body>
                        |<div hx-swap="outerHTML">
                        |responded
                        |</div>
                        |</body>
                    """.trimMargin()
                )
            )
        }
    }
}
