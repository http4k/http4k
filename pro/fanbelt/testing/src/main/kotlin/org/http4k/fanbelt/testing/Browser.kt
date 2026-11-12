/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.testing

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.then
import org.http4k.fanbelt.infrastructure.Node
import org.http4k.testing.toHttpHandler
import org.http4k.webdriver.datastar.DatastarWebDriver
import org.http4k.webdriver.datastar.ScriptNavigation
import org.openqa.selenium.By

fun browser(app: Node, scheduler: DeterministicScheduler, cookie: Cookie? = null) = DatastarWebDriver(
    Filter { next ->
        { request -> next(if (cookie == null) request else request.cookie(cookie)).also { scheduler.runUntilIdle() } }
    }.then(app.handler.toHttpHandler()),
    SchedulerClock(scheduler),
    plugins = listOf(ScriptNavigation, Effects),
)

fun DatastarWebDriver.verdict(error: By): Verdict {
    if (actions().lastOrNull()?.status == FORBIDDEN) return Verdict.Denied
    if (actions().lastOrNull()?.status == NOT_FOUND) return Verdict.Hidden
    val shown = findElements(error).map { it.text }.filter { it.isNotBlank() }.toSet()
    return if (shown.isEmpty()) Verdict.Accepted else Verdict.Rejected(shown)
}

fun Node.provesServerRefuses(path: String, cookie: Cookie, signals: String = "{}") = handler.toHttpHandler().provesServerRefuses(path, listOf(cookie), signals)

fun HttpHandler.provesServerRefuses(path: String, cookies: List<Cookie>, signals: String = "{}"): Verdict {
    val post = Request(POST, path).header("datastar-request", "true").header("content-type", "application/json").body(signals)
    val response = this(cookies.fold(post, Request::cookie))
    if (response.status == FORBIDDEN) return Verdict.Denied
    if (response.status == NOT_FOUND) return Verdict.Hidden
    val errors = Regex(""""errors":\{(.*?)}""").find(response.bodyString())?.groupValues?.get(1).orEmpty()
    val shown = Regex(""":"([^"]+)"""").findAll(errors).map { it.groupValues[1] }.toSet()
    check(shown.isNotEmpty()) { "$path is hidden from this viewer, but the server answered ${response.status}" }
    return Verdict.Rejected(shown)
}
