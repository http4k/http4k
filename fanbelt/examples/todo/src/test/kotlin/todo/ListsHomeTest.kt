package todo

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.startsWith
import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.fanbelt.testing.TestRuntime
import org.http4k.fanbelt.testing.toFanBeltBrowser
import org.http4k.testing.toHttpHandler
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import java.time.Instant.ofEpochMilli

class ListsHomeTest {
    private val scheduler = DeterministicScheduler(ofEpochMilli(0))
    private val app = App(TestRuntime(scheduler)).apply {
        start()
        handler.http!!(Request(POST, "/lists").form("list", "demo"))
    }
    private val browser = app.handler.toHttpHandler().toFanBeltBrowser(scheduler)

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
        assertThat(lists(), equalTo(listOf("demo")))

        browser.findElement(By.linkText("demo")).click()

        assertThat(Uri.of(browser.currentUrl.orEmpty()).path, equalTo("/lists/demo/"))
        assertThat(browser.findElement(By.cssSelector("#todos .remaining")).text.substringBefore(" "), equalTo("0"))
    }

    @Test
    fun `creating a list opens it, and it's listed from then on`() {
        browser.get("http://localhost/")

        create("groceries")

        assertThat(Uri.of(browser.currentUrl.orEmpty()).path, equalTo("/lists/groceries/"))
        browser.get("http://localhost/")
        assertThat(lists(), equalTo(listOf("demo", "groceries")))
    }

    @Test
    fun `a name that's taken is refused, rather than opening someone else's list`() {
        browser.get("http://localhost/")

        create("demo")

        assertThat(Uri.of(browser.currentUrl.orEmpty()).path, equalTo("/lists"))
        assertThat(browser.findElement(By.className("error")).text, equalTo("That name is taken"))
        assertThat(lists(), equalTo(listOf("demo")))
    }

    @Test
    fun `an invalid name is refused with a reason, and nothing is created`() {
        browser.get("http://localhost/")

        create("not a name")

        assertThat(browser.findElement(By.className("error")).text, startsWith("A list name is"))
        assertThat(lists(), equalTo(listOf("demo")))
    }
}
