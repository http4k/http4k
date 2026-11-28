package org.http4k.fanbelt.testing.playwright

import com.microsoft.playwright.Page
import com.microsoft.playwright.Response
import com.microsoft.playwright.TimeoutError
import dev.forkhandles.time.DeterministicScheduler
import org.http4k.config.Environment
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.cookie.Cookie
import org.http4k.fanbelt.infrastructure.FanbeltSettings.FANBELT_STREAM_KEEPALIVE
import org.http4k.fanbelt.testing.Verdict
import org.http4k.playwright.Http4kBrowser
import org.http4k.playwright.HttpPage
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.LockSupport
import kotlin.concurrent.thread
import com.microsoft.playwright.options.Cookie as PlaywrightCookie

fun Http4kBrowser.newPage(cookie: Cookie): HttpPage = newPage().apply {
    context().addCookies(listOf(PlaywrightCookie(cookie.name, cookie.value).setUrl(baseUri.toString())))
}

fun Page.posting(action: () -> Unit): Response {
    val response = waitForResponse({ it.request().method() == "POST" }, action)
    response.finished()
    evaluate("() => new Promise(done => requestAnimationFrame(() => done()))")
    return response
}

fun Page.verdictAt(error: String, action: () -> Unit): Verdict {
    val status = posting(action).status()
    if (status == FORBIDDEN.code) return Verdict.Denied
    if (status == NOT_FOUND.code) return Verdict.Hidden
    val shown = locator(error).allInnerTexts().filter { it.isNotBlank() }.toSet()
    return if (shown.isEmpty()) Verdict.Accepted else Verdict.Rejected(shown)
}

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

fun DeterministicScheduler.noticeClosedTabs() = repeat(3) {
    Thread.sleep(20)
    tick(FANBELT_STREAM_KEEPALIVE(Environment.EMPTY))
}

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
