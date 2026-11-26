package org.http4k.fanbelt.testing.webdriver

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.Verdict.Accepted
import org.http4k.fanbelt.testing.Verdict.Denied
import org.http4k.fanbelt.testing.Verdict.Hidden
import org.http4k.fanbelt.testing.Verdict.Rejected
import org.http4k.webdriver.datastar.DatastarPlugin
import org.http4k.webdriver.datastar.DatastarWebDriver
import org.openqa.selenium.By

/**
 * A headless Datastar browser over the app for one user. Each request runs the scheduler until idle.
 */
fun HttpHandler.toFanBeltBrowser(
    scheduler: DeterministicScheduler,
    plugins: List<DatastarPlugin> = emptyList()
) = DatastarWebDriver(
    { request: Request -> this(request).also { scheduler.runUntilIdle() } }, scheduler.toClock(), plugins = plugins,
)

/**
 * The Verdict of the last action: a 403 is Denied, a 404 Hidden, errors shown are Rejected, otherwise Accepted.
 */
fun DatastarWebDriver.verdictAt(error: By): Verdict {
    if (actions().lastOrNull()?.status == FORBIDDEN) return Denied
    if (actions().lastOrNull()?.status == NOT_FOUND) return Hidden
    val shown = findElements(error).map { it.text }.filter { it.isNotBlank() }.toSet()
    return if (shown.isEmpty()) Accepted else Rejected(shown)
}
