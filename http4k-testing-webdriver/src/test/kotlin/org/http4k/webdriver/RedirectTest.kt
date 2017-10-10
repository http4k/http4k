package org.http4k.webdriver

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasElement
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.cookie.cookie
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.Test
import org.openqa.selenium.Cookie
import org.http4k.core.cookie.Cookie as HCookie

class RedirectTest {

    private val finalURI = "/final-destination"
    private val cookieKey = "http4k"
    private val cookieValue = "hello, cookie. give me more cookie"

    private val redirectingHandler = routes(
        finalURI bind Method.GET to { _: Request -> Response(OK).body("You made it!") },
        "/" bind Method.GET to { _: Request ->
            Response(SEE_OTHER)
                .header("Location", finalURI)
                .cookie(org.http4k.core.cookie.Cookie(cookieKey, cookieValue))
        }
    )

    private val driver = Http4kWebDriver(redirectingHandler)

    @Test
    fun `follows redirects and sets cookies`() {
        driver.get("/")

        assertThat(driver.currentUrl, equalTo(finalURI))

        val expectedCookie = Cookie(cookieKey, cookieValue)
        assertThat(driver.manage().cookies, hasElement(expectedCookie))
    }

}