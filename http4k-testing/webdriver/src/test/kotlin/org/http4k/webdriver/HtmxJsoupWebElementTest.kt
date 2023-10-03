package org.http4k.webdriver

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.body.form
import org.http4k.core.then
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.openqa.selenium.By

class HtmxJsoupWebElementTest {

    private var newLocation: Pair<Method, String>? = null
    private val navigate: (Request) -> Unit = { it -> newLocation = it.method to it.uri.toString() }
    private val getURL: () -> String? = { null }
    private val contentTypePlainTextFilter = Filter { next: HttpHandler ->
        { next(it).header("Content-Type", "text/plain") }
    }

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
        val handler: HttpHandler = contentTypePlainTextFilter.then { req ->
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
        private val alwaysRespondHandler: HttpHandler =
            contentTypePlainTextFilter.then{ Response(Status.OK).body("responded") }

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
        fun `targets a sibling by class`() {
            val html = Jsoup.parse(
                """
                    |<body>
                    |<div id="foo" hx-get="/test" hx-target=".bar">foo</div>
                    |<div class="bar">bar</div>
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
                        |<div id="foo" hx-get="/test" hx-target=".bar">foo</div>
                        |<div class="bar">responded</div>
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

    @Nested
//    @Disabled("Work in progress")
    inner class `Handles form data` {
        @Test
        fun `single input field`() {
            val getHandler: HttpHandler = contentTypePlainTextFilter.then { req ->
                if (req.method == Method.GET) {
                    Response(Status.OK)
                        .body(req.query("name") ?: "NONE")
                } else {
                    Response(Status.BAD_REQUEST).body("expected a GET request, but was ${req.method}")
                }
            }

            val html = Jsoup.parse(
                """
                    |<body>
                    |<input name="name" hx-get="/test" hx-swap="outerHTML"/>
                    |</body>
                """.trimMargin()
            ).outputSettings(jsoupOutputSettings)

            val element = HtmxJsoupWebElement(JSoupWebElement(navigate, getURL, html), getHandler)

            val body = element.findElement(By.tagName("body"))

            element.findElement(By.tagName("input"))!!.sendKeys("keys")

            assertThat(
                body.toString(),
                equalTo(
                    """
                    |<body>
                    |keys
                    |</body>
                    """.trimMargin()
                )
            )
        }

        @Test
        fun `post a form`() {
            val getHandler: HttpHandler = contentTypePlainTextFilter.then { req ->
                if (req.method == Method.POST) {
                    Response(Status.OK)
                        .body(req.form().joinToString { "${it.first}: ${it.second}" })
                } else {
                    Response(Status.BAD_REQUEST).body("expected a POST request, but was ${req.method}")
                }
            }

            val html = Jsoup.parse(
                """
                    |<body>
                    |<form hx-post="/" hx-swap="outerHtml">
                    |<input type="text" id="foo1" name="foo1" value="bar1"/>
                    |<input type="text" id="foo2" name="foo2" value="bar2"/>
                    |</form>
                    |</body>
                """.trimMargin()
            ).outputSettings(jsoupOutputSettings)

            val element = HtmxJsoupWebElement(JSoupWebElement(navigate, getURL, html), getHandler)

            val body = element.findElement(By.tagName("body"))

            element.findElement(By.tagName("form"))!!.submit()

            assertThat(
                body.toString(),
                equalTo(
                    """
                    |<body>
                    |foo1: bar1, foo2: bar2
                    |</body>
                    """.trimMargin()
                )
            )
        }
    }
}
