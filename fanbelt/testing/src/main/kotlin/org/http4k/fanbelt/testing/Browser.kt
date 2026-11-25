package org.http4k.fanbelt.testing

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.fanbelt.infrastructure.Node
import org.http4k.fanbelt.testing.Verdict.Accepted
import org.http4k.fanbelt.testing.Verdict.Denied
import org.http4k.fanbelt.testing.Verdict.Hidden
import org.http4k.fanbelt.testing.Verdict.Rejected
import org.http4k.testing.toHttpHandler
import org.http4k.webdriver.datastar.DatastarPlugin
import org.http4k.webdriver.datastar.DatastarWebDriver
import org.http4k.webdriver.datastar.ScriptNavigation
import org.openqa.selenium.By

/**
 * A headless Datastar browser over the app for one user. Each request runs the scheduler until idle.
 */
fun HttpHandler.toFanBeltBrowser(
    scheduler: DeterministicScheduler,
    plugins: List<DatastarPlugin> = listOf(ScriptNavigation, Effects)
) = DatastarWebDriver(
    { request: Request -> this(request).also { scheduler.runUntilIdle() } }, scheduler.toClock(), plugins = plugins,
)

/**
 * The Verdict of the last action: a 403 is Denied, a 404 Hidden, errors shown are Rejected, otherwise Accepted.
 */
fun DatastarWebDriver.verdict(error: By): Verdict {
    if (actions().lastOrNull()?.status == FORBIDDEN) return Denied
    if (actions().lastOrNull()?.status == NOT_FOUND) return Hidden
    val shown = findElements(error).map { it.text }.filter { it.isNotBlank() }.toSet()
    return if (shown.isEmpty()) Accepted else Rejected(shown)
}

/**
 * Posts an action the page hides from this viewer and asserts the server refuses it.
 */
fun Node.provesServerRefuses(path: String, cookie: Cookie, signals: String = "{}") = handler.toHttpHandler().provesServerRefuses(path, listOf(cookie), signals)

/**
 * Posts an action as Datastar would and returns how the server refused it. Fails if the server accepts it.
 */
fun HttpHandler.provesServerRefuses(path: String, cookies: List<Cookie>, signals: String = "{}"): Verdict {
    val post = Request(POST, path).header("datastar-request", "true").header("content-type", "application/json").body(signals)
    val response = this(cookies.fold(post, Request::cookie))
    if (response.status == FORBIDDEN) return Denied
    if (response.status == NOT_FOUND) return Hidden
    val errors = Regex(""""errors":\{(.*?)}""").find(response.bodyString())?.groupValues?.get(1).orEmpty()
    val shown = Regex(""":"([^"]+)"""").findAll(errors).map { it.groupValues[1] }.toSet()
    check(shown.isNotEmpty()) { "$path is hidden from this viewer, but the server answered ${response.status}" }
    return Rejected(shown)
}
