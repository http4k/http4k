package org.http4k.playwright

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class RunWithPlaywrightTest {

    private val app = routes(
        "/" bind GET to { _: Request -> Response(OK).body("helloworld") }
    )

    @RegisterExtension
    val playwright = RunWithPlaywright(app)

    @Test
    fun `page is interacted with`(browser: Http4kBrowser) {
        with(browser.newPage()) {
            assertThat(String(navigate().body()), equalTo("helloworld"))
        }
    }
}
