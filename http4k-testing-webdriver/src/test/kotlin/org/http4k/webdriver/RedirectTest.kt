package org.http4k.webdriver

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.cookie.cookie
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.Ignore
import org.junit.Test
import org.openqa.selenium.Cookie
import org.http4k.core.cookie.Cookie as HCookie

class RedirectTest {

    private val redirectingHandler = routes(
        "/final-destination" bind Method.GET to { _: Request -> Response(OK).body("You made it!") },
        "/" bind Method.GET to { _: Request -> Response(SEE_OTHER)
            .header("Location", "/final-destination")
            .cookie(org.http4k.core.cookie.Cookie("http4k", "hello, cookie"))
        }
    )

    private val driver = Http4kWebDriver(redirectingHandler)

    @Test
    fun `follows redirects and sets cookies`() {
        driver.get("/")
        assertThat(driver.currentUrl, equalTo("/final-destination"))
        assertThat(driver.manage().cookies.contains(Cookie("http4k", "hello, cookie")), equalTo(true)) //todo: bleh
    }

}