/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.testing

import com.microsoft.playwright.Browser
import com.microsoft.playwright.Browser.NewContextOptions
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.Response
import com.microsoft.playwright.TimeoutError
import org.http4k.client.JavaHttpClient
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Uri
import org.http4k.core.cookie.Cookie
import org.http4k.core.then
import org.http4k.fanbelt.infrastructure.Lifecycle
import org.http4k.fanbelt.infrastructure.VirtualThreadScheduler
import org.http4k.filter.ClientFilters.SetBaseUriFrom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import com.microsoft.playwright.options.Cookie as PlaywrightCookie

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

fun Page.posting(action: () -> Unit): Response {
    val response = waitForResponse({ it.request().method() == "POST" }, action)
    response.finished()
    evaluate("() => new Promise(done => requestAnimationFrame(() => done()))")
    return response
}

fun Page.provesServerRefuses(path: String, signals: String = "{}") =
    SetBaseUriFrom(Uri.of(url()).path("").query("")).then(JavaHttpClient())
        .provesServerRefuses(path, context().cookies().map { Cookie(it.name, it.value) }, signals)

fun Page.verdict(error: String, action: () -> Unit): Verdict {
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

class MovableClock(private val real: Clock = Clock.systemUTC()) : Clock() {
    private val moved = AtomicReference(Duration.ZERO)
    private val schedulers = CopyOnWriteArrayList<MovableScheduler>()

    fun tick(by: Duration) {
        moved.updateAndGet { it + by }
        schedulers.forEach(MovableScheduler::runDue)
    }

    internal fun moves(scheduler: MovableScheduler) {
        schedulers += scheduler
    }

    override fun getZone(): ZoneId = real.zone

    override fun withZone(zone: ZoneId) = this

    override fun instant(): Instant = real.instant() + moved.get()
}

class MovableScheduler(
    private val clock: MovableClock,
    private val real: ScheduledExecutorService = VirtualThreadScheduler(),
) : ScheduledExecutorService by real {
    private class Timer(val at: Instant, val run: Runnable, val future: ScheduledFuture<*>)

    private val timers = CopyOnWriteArrayList<Timer>()

    init {
        clock.moves(this)
    }

    override fun schedule(command: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture<*> {
        val started = AtomicBoolean()
        val once = Runnable { if (started.compareAndSet(false, true)) command.run() }
        val future = real.schedule(once, delay, unit)
        timers += Timer(clock.instant().plusNanos(unit.toNanos(delay)), once, future)
        return future
    }

    internal fun runDue() {
        val now = clock.instant()
        val due = timers.filter { it.at <= now }
        timers -= due
        due.filterNot { it.future.isCancelled }.forEach { real.execute(it.run) }
    }
}
