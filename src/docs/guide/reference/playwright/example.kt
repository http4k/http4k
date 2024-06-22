package guide.reference.playwright

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.playwright.Http4kBrowser
import org.http4k.playwright.LaunchPlaywrightBrowser
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class LaunchPlaywrightBrowserTest {

    private val app = routes(
        "/foo" bind GET to { _: Request -> Response(OK).body("foo") },
        "/redirect" bind GET to { _: Request -> Response(Status.FOUND).with(Header.LOCATION of Uri.of("https://example.com")) },
        "/" bind GET to { _: Request -> Response(OK).body("helloworld") }
    )

    @RegisterExtension
    // this defaults to Chromium and the app is launched on a random port
    val playwright = LaunchPlaywrightBrowser(app)

    @Test
    fun `can check browser interactions`(browser: Http4kBrowser) {
        with(browser.newPage()) {
            // you don't have to specify the entire URL of the application paths - this is handled automatically
            assertThat(String(navigateHome().body()), equalTo("helloworld"))
            assertThat(String(navigate("/foo").body()), equalTo("foo"))
            assertThat(String(navigate("/redirect").body()), containsSubstring("Example Domain"))
            assertThat(String(navigate("http://google.com").body()), containsSubstring("google"))
        }
    }
}
