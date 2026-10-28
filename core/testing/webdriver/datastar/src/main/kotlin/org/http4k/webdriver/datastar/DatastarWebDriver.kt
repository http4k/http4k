package org.http4k.webdriver.datastar

import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.relative
import org.http4k.webdriver.Http4kWebDriver
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import java.time.Clock
import java.util.concurrent.atomic.AtomicReference

/**
 * A headless, pure-Kotlin approximation of a browser running datastar v1: an Http4kWebDriver
 * with a DatastarBehaviour plugged in, so navigation, history and cookies all work as standard.
 * [plugins] add to the parts of datastar it models, or replace a built-in plugin of the same name.
 */
class DatastarWebDriver private constructor(
    private val browser: Http4kWebDriver,
    private val behaviour: DatastarBehaviour
) : WebDriver by browser {

    constructor(
        http: HttpHandler,
        clock: Clock = Clock.systemUTC(),
        autoSettle: Boolean = true,
        plugins: List<DatastarPlugin> = emptyList(),
    ) : this(http, clock, autoSettle, plugins, AtomicReference(), AtomicReference())

    private constructor(
        http: HttpHandler,
        clock: Clock,
        autoSettle: Boolean,
        plugins: List<DatastarPlugin>,
        captured: AtomicReference<DatastarBehaviour>,
        browser: AtomicReference<Http4kWebDriver>,
    ) : this(
        Http4kWebDriver({ request -> http(request).also { captured.get().responded(request, it) } }, clock) {
            DatastarBehaviour(it, autoSettle, plugins) { destination ->
                browser.get().get(Uri.of(browser.get().currentUrl.orEmpty()).relative(destination))
            }.also(captured::set)
        }.also(browser::set),
        captured.get()
    )

    fun settle() = behaviour.settle()

    fun actions(): List<ActionResult> = behaviour.actions()

    override fun getPageSource(): String =
        browser.findElements(By.tagName("html")).firstOrNull()?.getDomProperty("outerHTML").orEmpty()
}
