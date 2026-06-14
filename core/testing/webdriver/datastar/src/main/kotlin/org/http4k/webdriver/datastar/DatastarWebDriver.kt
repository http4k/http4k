package org.http4k.webdriver.datastar

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

    constructor(http: HttpHandler, clock: Clock = Clock.systemUTC()) :
        this(DatastarBehaviour(http), http, clock)

    override fun getPageSource() = behaviour.pageSource()
}
