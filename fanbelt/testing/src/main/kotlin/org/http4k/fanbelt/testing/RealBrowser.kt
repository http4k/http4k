package org.http4k.fanbelt.testing

import com.microsoft.playwright.Browser
import com.microsoft.playwright.Browser.NewContextOptions
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.Response
import com.microsoft.playwright.TimeoutError
import dev.forkhandles.time.DeterministicScheduler
import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Uri
import org.http4k.core.cookie.Cookie
import org.http4k.core.then
import org.http4k.fanbelt.infrastructure.FanbeltSettings.FANBELT_STREAM_KEEPALIVE
import org.http4k.fanbelt.infrastructure.Lifecycle
import org.http4k.filter.ClientFilters.SetBaseUriFrom
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.LockSupport
import kotlin.concurrent.thread
import com.microsoft.playwright.options.Cookie as PlaywrightCookie

/**
 * Playwright Chromium against a running app. Open one context per user.
 */
class RealBrowser(private val base: Uri) : Lifecycle {
    private lateinit var playwright: Playwright
    private lateinit var chromium: Browser

    override fun start() {
        playwright = Playwright.create()
        chromium = playwright.chromium().launch()
    }

    fun open(path: String, cookie: Cookie? = null): Page = blank(cookie).apply { navigate(path) }

    fun blank(cookie: Cookie? = null): Page {
        val context = chromium.newContext(NewContextOptions().setBaseURL(base.toString()))
        if (cookie != null) context.addCookies(listOf(PlaywrightCookie(cookie.name, cookie.value).setUrl(base.toString())))
        return context.newPage()
    }

    override fun close() {
        if (::playwright.isInitialized) playwright.close()
    }
}

/**
 * Runs an action that posts and waits until the page has been patched.
 */
fun Page.posting(action: () -> Unit): Response {
    val response = waitForResponse({ it.request().method() == "POST" }, action)
    response.finished()
    evaluate("() => new Promise(done => requestAnimationFrame(() => done()))")
    return response
}

/**
 * Posts an action the page hides and asserts the server refuses it.
 */
fun Page.provesServerRefuses(path: String, signals: String = "{}") =
    SetBaseUriFrom(Uri.of(url()).path("").query("")).then(JavaHttpClient())
        .provesServerRefuses(path, context().cookies().map { Cookie(it.name, it.value) }, signals)

/**
 * Runs an action that posts and returns its Verdict.
 */
fun Page.verdict(error: String, action: () -> Unit): Verdict {
    val status = posting(action).status()
    if (status == FORBIDDEN.code) return Verdict.Denied
    if (status == NOT_FOUND.code) return Verdict.Hidden
    val shown = locator(error).allInnerTexts().filter { it.isNotBlank() }.toSet()
    return if (shown.isEmpty()) Verdict.Accepted else Verdict.Rejected(shown)
}

/**
 * Runs an action that should navigate: Accepted if it arrives, otherwise how it was refused.
 */
fun Page.navigates(error: String, to: String, action: () -> Unit): Verdict {
    val from = url()
    val status = waitForResponse({ it.request().method() == "POST" }, action).status()
    if (status == FORBIDDEN.code) return Verdict.Denied
    if (status == NOT_FOUND.code) return Verdict.Hidden
    return try {
        waitForURL({ it != from && it.endsWith(to) }, Page.WaitForURLOptions().setTimeout(1000.0))
        Verdict.Accepted
    } catch (_: TimeoutError) {
        Verdict.Rejected(locator(error).allInnerTexts().filter { it.isNotBlank() }.toSet())
    }
}

/**
 * Ticks through enough keepalive pings for the server to notice a tab the browser has closed: the first ping to a
 * closed socket is answered with a reset, and the next one fails.
 */
fun DeterministicScheduler.noticeClosedTabs() = repeat(3) {
    Thread.sleep(20)
    tick(FANBELT_STREAM_KEEPALIVE(Environment.EMPTY))
}

/**
 * Runs due work in the background as other threads queue it, without moving time, until closed.
 */
fun DeterministicScheduler.pumping(): AutoCloseable {
    val pumping = AtomicBoolean(true)
    val pump = thread(isDaemon = true, name = "deterministic-scheduler-pump") {
        while (pumping.get()) {
            runUntilIdle()
            LockSupport.parkNanos(MILLISECONDS.toNanos(1))
        }
    }
    return AutoCloseable {
        pumping.set(false)
        pump.join()
    }
}
