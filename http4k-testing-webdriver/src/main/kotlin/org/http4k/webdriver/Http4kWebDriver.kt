package org.http4k.webdriver


import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.openqa.selenium.Alert
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebDriver.Navigation
import org.openqa.selenium.WebElement
import java.net.URL
import java.util.*
import kotlin.NoSuchElementException

typealias Navigate = (Method, String, String) -> Unit

class Http4kWebDriver(private val handler: HttpHandler) : WebDriver {

    private var current: Page? = null
    private var activeElement: WebElement? = null

    private fun navigateTo(method: Method, url: String, body: String) {
        current = Page(this::navigateTo, UUID.randomUUID(), url, handler(Request(method, url).body(body)).bodyString(), current)
    }

    override fun get(url: String) {
        navigateTo(Method.GET, url, "")
    }

    override fun getCurrentUrl(): String? = current?.url

    override fun getTitle(): String? = current?.title

    override fun findElements(by: By): List<WebElement>? = current?.findElements(by)

    override fun findElement(by: By): WebElement? = current?.findElements(by)?.firstOrNull()

    override fun getPageSource(): String? = current?.contents

    override fun close() {
        current = null
    }

    override fun quit() {
        current = null
    }

    override fun getWindowHandles(): Set<String> = current?.let { setOf(it.handle.toString()) } ?: emptySet()

    override fun getWindowHandle(): String? = windowHandles.firstOrNull()

    override fun switchTo(): WebDriver.TargetLocator {
        val driver = this
        return object : WebDriver.TargetLocator {
            override fun frame(index: Int): WebDriver = throw FeatureNotImplementedYet

            override fun frame(nameOrId: String?): WebDriver = throw FeatureNotImplementedYet

            override fun frame(frameElement: WebElement?): WebDriver = throw FeatureNotImplementedYet

            override fun parentFrame(): WebDriver = throw FeatureNotImplementedYet

            override fun alert(): Alert = throw FeatureNotImplementedYet

            override fun activeElement(): WebElement = activeElement ?: current?.firstElement() ?: throw NoSuchElementException("no page loaded!")

            override fun window(nameOrHandle: String?): WebDriver = if (current?.handle?.toString() != nameOrHandle) throw NoSuchElementException("window with handle" + nameOrHandle) else driver

            override fun defaultContent(): WebDriver = driver
        }
    }

    override fun navigate(): Navigation = object : Navigation {
        override fun to(url: String) = get(url)

        override fun to(url: URL) = get(url.toString())

        override fun forward() {
            current?.next?.let { current = it }
        }

        override fun refresh() {
            current?.let {
                current = it.copy(contents = handler(Request(Method.GET, it.url)).bodyString())
            }
        }

        override fun back() {
            current?.previous?.let { current = it.copy(next = current) }
        }
    }

    override fun manage(): WebDriver.Options? = throw FeatureNotImplementedYet
}
