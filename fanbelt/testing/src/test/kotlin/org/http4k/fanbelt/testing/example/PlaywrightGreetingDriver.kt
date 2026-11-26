package org.http4k.fanbelt.testing.example

import com.microsoft.playwright.Playwright
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.playwright.newPage
import org.http4k.fanbelt.testing.playwright.verdictAt
import org.http4k.playwright.Http4kBrowser
import org.http4k.server.Jetty
import org.http4k.server.uri
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.AfterEach

class PlaywrightGreetingDriver : GreetingDriver, AutoCloseable {
    private val server = GreetingApp().asServer(Jetty(0)).start()
    private val playwright = Playwright.create()
    private val browser = Http4kBrowser(playwright.chromium().launch(), server.uri())

    override fun visitor(name: String): Visitor = object : Visitor {
        private val page by lazy { browser.newPage(personCookie(PersonName.of(name))).apply { navigate("/acme/") } }

        override fun watches() {
            page
        }

        override fun says(text: String) = page.verdictAt(".error") {
            page.locator("input.text").fill(text)
            page.locator("input.text").press("Enter")
        }

        override fun clears(): Verdict {
            val button = page.locator("button.clear")
            if (button.count() == 0) return Verdict.Unavailable
            return page.verdictAt(".error") { button.click() }
        }

        override fun sees(): String? = page.locator("#greeting .text").takeIf { it.count() > 0 }?.innerText()
    }

    override fun close() {
        playwright.close()
        server.stop()
    }
}

class GreetingPlaywrightTest : GreetingContract, PortBasedTest {
    override val driver = PlaywrightGreetingDriver()

    @AfterEach
    fun close() = driver.close()
}
