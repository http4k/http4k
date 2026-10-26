package org.http4k.webdriver.datastar

import org.http4k.core.HttpHandler
import org.http4k.webdriver.Http4kWebDriver
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import java.time.Clock
import java.util.concurrent.atomic.AtomicReference

/**
 * A headless, pure-Kotlin approximation of a browser running datastar v1: an Http4kWebDriver
 * with a DatastarBehaviour plugged in, so navigation, history and cookies all work as standard.
 */
class DatastarWebDriver private constructor(
    private val browser: Http4kWebDriver,
    private val behaviour: DatastarBehaviour
) : WebDriver by browser {

    constructor(http: HttpHandler, clock: Clock = Clock.systemUTC(), autoSettle: Boolean = true) :
        this(http, clock, autoSettle, AtomicReference())

    private constructor(http: HttpHandler, clock: Clock, autoSettle: Boolean, captured: AtomicReference<DatastarBehaviour>) :
        this(
            Http4kWebDriver({ request -> http(request).also { captured.get().responded(request, it) } }, clock) {
                DatastarBehaviour(it, autoSettle).also(captured::set)
            },
            captured.get()
        )

    fun settle() = behaviour.settle()

    fun actions(): List<ActionResult> = behaviour.actions()

    override fun getPageSource(): String =
        browser.findElements(By.tagName("html")).firstOrNull()?.getDomProperty("outerHTML").orEmpty()
}
