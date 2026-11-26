package org.http4k.fanbelt.testing.example

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.then
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.infrastructure.TestRuntime
import org.http4k.fanbelt.testing.provesServerRefuses
import org.http4k.fanbelt.testing.webdriver.toFanBeltBrowser
import org.http4k.fanbelt.testing.webdriver.verdict
import org.http4k.filter.ClientFilters
import org.http4k.testing.toHttpHandler
import org.openqa.selenium.By
import java.time.Instant.ofEpochMilli

class UiGreetingDriver : GreetingDriver {
    private val scheduler = DeterministicScheduler(ofEpochMilli(0))
    private val node = GreetingApp(TestRuntime(scheduler)).apply { start() }

    override fun visitor(name: String): Visitor = object : Visitor {
        private val cookie = personCookie(PersonName.of(name))
        private val browser = ClientFilters.SetCookie(cookie).then(node.handler.toHttpHandler()).toFanBeltBrowser(scheduler)
        private var visited = false

        override fun watches() = visit()

        override fun says(text: String): Verdict {
            visit()
            browser.findElement(By.cssSelector("input.text")).run {
                clear()
                sendKeys(text)
                submit()
            }
            return browser.verdict(By.cssSelector(".error"))
        }

        override fun clears(): Verdict {
            visit()
            val button = browser.findElements(By.cssSelector("button.clear")).firstOrNull()
                ?: return node.provesServerRefuses("/acme/clear", cookie)
            button.click()
            return browser.verdict(By.cssSelector(".error"))
        }

        override fun sees(): String? {
            visit()
            return browser.findElements(By.cssSelector("#greeting .text")).firstOrNull()?.text
        }

        private fun visit() {
            if (visited) return
            browser.get("http://localhost/acme/")
            visited = true
        }
    }
}

class GreetingUiTest : GreetingContract {
    override val driver = UiGreetingDriver()
}
