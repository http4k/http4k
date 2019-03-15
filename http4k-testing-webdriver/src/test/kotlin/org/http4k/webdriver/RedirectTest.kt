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
import org.http4k.core.cookie.cookies
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import org.openqa.selenium.Cookie
import org.http4k.core.cookie.Cookie as HCookie

class RedirectTest {

    private val startingUrl = "/"
    private val finalUrl = "/final-destination"
    private val cookieKey = "http4k"
    private val cookieValue = "hello, cookie. give me more cookie"

    private var cookiesSentToFinalDestination = listOf<HCookie>()

    private val redirectingHandler = routes(
        finalUrl bind Method.GET to { req: Request ->
            {
                cookiesSentToFinalDestination = req.cookies()
                Response(OK).body("You made it!")
            }()
        },
        startingUrl bind Method.GET to {
            Response(SEE_OTHER)
                .header("Location", finalUrl)
                .cookie(org.http4k.core.cookie.Cookie(cookieKey, cookieValue))
        }
    )

    private val driver = Http4kWebDriver(redirectingHandler)

    @Test
    fun `follows redirects and sets cookies`() {
        val someOtherCookie = Cookie("foo", "bar")
        val redirectAddedCookie = Cookie(cookieKey, cookieValue)

        driver.manage().addCookie(someOtherCookie)

        driver.get(startingUrl)

        assertThat(driver.currentUrl, equalTo(finalUrl))
        assertThat(driver.manage().cookies, hasElement(redirectAddedCookie))

        val expectedCookiesInLastRequest = cookiesSentToFinalDestination.map { it.toSeleniumCookie() }.toSet()
        assertThat(
            expectedCookiesInLastRequest,
            equalTo(setOf(redirectAddedCookie, someOtherCookie))
        )
    }

    private fun HCookie.toSeleniumCookie() = Cookie(name, value, path)

}