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

fun HttpHandler.toFanBeltBrowser(
    scheduler: DeterministicScheduler,
    plugins: List<DatastarPlugin> = emptyList()
) = DatastarWebDriver(
    { request: Request -> this(request).also { scheduler.runUntilIdle() } }, scheduler.toClock(), plugins = plugins,
)

fun DatastarWebDriver.verdictAt(error: By): Verdict {
    if (actions().lastOrNull()?.status == FORBIDDEN) return Denied
    if (actions().lastOrNull()?.status == NOT_FOUND) return Hidden
    val shown = findElements(error).map { it.text }.filter { it.isNotBlank() }.toSet()
    return if (shown.isEmpty()) Accepted else Rejected(shown)
}
