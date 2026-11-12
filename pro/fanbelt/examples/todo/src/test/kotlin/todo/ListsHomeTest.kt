package todo

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.fanbelt.testing.TestRuntime
import org.http4k.fanbelt.testing.browser
import org.http4k.fanbelt.testing.start
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openqa.selenium.By

class ListsHomeTest {
    private val scheduler = DeterministicScheduler(start)
    private val app = App(TestRuntime(scheduler)).apply {
        start()
        handler.http!!(Request(POST, "/lists").form("list", "demo"))
    }
    private val browser = browser(app, scheduler)

    private fun lists() = browser.findElements(By.cssSelector("#lists a")).map { it.text }

    private fun create(name: String) {
        browser.findElement(By.name("list")).run {
            clear()
            sendKeys(name)
            submit()
        }
    }

    @Test
    fun `the home page lists the demo list and opens it`() {
        browser.get("http://localhost/")
        assertEquals(listOf("demo"), lists())

        browser.findElement(By.linkText("demo")).click()

        assertEquals("/lists/demo/", Uri.of(browser.currentUrl.orEmpty()).path)
        assertEquals("0", browser.findElement(By.cssSelector("#todos .remaining")).text.substringBefore(" "))
    }

    @Test
    fun `creating a list opens it, and it's listed from then on`() {
        browser.get("http://localhost/")

        create("groceries")

        assertEquals("/lists/groceries/", Uri.of(browser.currentUrl.orEmpty()).path)
        browser.get("http://localhost/")
        assertEquals(listOf("demo", "groceries"), lists())
    }

    @Test
    fun `a name that's taken is refused, rather than opening someone else's list`() {
        browser.get("http://localhost/")

        create("demo")

        assertEquals("/lists", Uri.of(browser.currentUrl.orEmpty()).path)
        assertEquals("That name is taken", browser.findElement(By.className("error")).text)
        assertEquals(listOf("demo"), lists())
    }

    @Test
    fun `an invalid name is refused with a reason, and nothing is created`() {
        browser.get("http://localhost/")

        create("not a name")

        assertEquals(true, browser.findElement(By.className("error")).text.startsWith("A list name is"))
        assertEquals(listOf("demo"), lists())
    }
}