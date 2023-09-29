package org.http4k.webdriver

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import org.openqa.selenium.By

class HtmxJsoupWebElementTest {

    private var newLocation: Pair<Method, String>? = null
    private val navigate: (Request) -> Unit = { it -> newLocation = it.method to it.uri.toString() }
    private val getURL: () -> String? = { null }

    val handler: HttpHandler = {
        Response(Status.OK).body("bar")
    }

    @Test
    fun `issues a GET request on click and swaps content`() {
        val div = HtmxJsoupWebElement(
            JSoupWebElement(
                navigate,
                getURL,
                Jsoup.parse(
                    """<div hx-get="/test">foo</div>"""
                )
            ),
            handler
        ).findElement(By.tagName("div"))!!

        div.click()

        assertThat(div.text, equalTo("bar"))
        assertThat(div.tagName, equalTo("div"))
    }
}
