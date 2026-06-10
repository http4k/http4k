package org.http4k.storyboard.datastar

import org.http4k.core.HttpHandler
import org.http4k.webdriver.Http4kWebDriver
import org.openqa.selenium.WebDriver
import java.time.Clock

/**
 * A headless, pure-Kotlin approximation of a browser running datastar v1: an Http4kWebDriver
 * with a DatastarBehaviour plugged in, so navigation, history and cookies all work as standard.
 */
class DatastarWebDriver private constructor(
    private val behaviour: DatastarBehaviour,
    handler: HttpHandler,
    clock: Clock
) : WebDriver by Http4kWebDriver(handler, clock, behaviour) {

    constructor(handler: HttpHandler, clock: Clock = Clock.systemDefaultZone()) :
        this(DatastarBehaviour(handler), handler, clock)

    /** The page source reflects the live, morphed document rather than the originally served HTML. */
    override fun getPageSource(): String = behaviour.pageSource()
}
