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
 * [extensions] teach it about elements a patch carries that datastar itself doesn't model.
 */
class DatastarWebDriver private constructor(
    private val browser: Http4kWebDriver,
    private val behaviour: DatastarBehaviour
) : WebDriver by browser {

    constructor(
        http: HttpHandler,
        clock: Clock = Clock.systemUTC(),
        autoSettle: Boolean = true,
        extensions: List<DatastarExtension> = emptyList(),
    ) : this(http, clock, autoSettle, extensions, AtomicReference(), AtomicReference())

    private constructor(
        http: HttpHandler,
        clock: Clock,
        autoSettle: Boolean,
        extensions: List<DatastarExtension>,
        captured: AtomicReference<DatastarBehaviour>,
        browser: AtomicReference<Http4kWebDriver>,
    ) : this(
        Http4kWebDriver({ request -> http(request).also { captured.get().responded(request, it) } }, clock) {
            DatastarBehaviour(it, autoSettle, extensions) { destination ->
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
