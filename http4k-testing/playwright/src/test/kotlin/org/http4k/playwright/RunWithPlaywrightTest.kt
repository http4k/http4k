package org.http4k.playwright

import com.microsoft.playwright.Browser
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class RunWithPlaywrightTest {

    private val app = routes(
        "/foo" bind GET to { _: Request -> Response(OK).body("foo") },
        "/redirect" bind GET to { _: Request -> Response(FOUND).with(Header.LOCATION of Uri.of("https://example.com")) },
        "/" bind GET to { _: Request -> Response(OK).body("helloworld") }
    )

    @RegisterExtension
    val playwright = RunWithPlaywright(app)

    @Test
    fun `provides http4k browser`(browser: Http4kBrowser) {
        with(browser.newPage()) {
            assertThat(String(navigateHome().body()), equalTo("helloworld"))
            assertThat(String(navigate("/foo").body()), equalTo("foo"))
            assertThat(String(navigate("/redirect").body()), containsSubstring("Example Domain"))
            assertThat(String(navigate("http://google.com").body()), containsSubstring("google"))
        }
    }

    @Test
    fun `provides browser`(browser: Browser) {
        with(browser.newPage()) {
            assertThat(String(navigate("/foo").body()), equalTo("foo"))
            assertThat(String(navigate("/redirect").body()), containsSubstring("Example Domain"))
            assertThat(String(navigate("http://google.com").body()), containsSubstring("google"))
        }
    }
}
