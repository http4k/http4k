/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.page.Page
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset.UTC
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit.SECONDS

val acme: TenantId = TenantId.of("acme")

fun acmeWorkspace(infra: AppInfra) = Workspace(infra, "/", { true }, { acme }, { Uri.of("/") })

class RecordingSse(
    override val connectRequest: Request,
    private val dead: Boolean = false,
    private val sending: (SseMessage.Event) -> Unit = {},
) : Sse {
    private val sent = CopyOnWriteArrayList<SseMessage>()
    private val closing = CopyOnWriteArrayList<() -> Unit>()

    @Volatile
    var closed = false
        private set

    val messages: List<SseMessage> get() = sent.toList()
    val events get() = messages.filterIsInstance<SseMessage.Event>()
    val received get() = events.map { it.toMessage() }
    val pings get() = messages.count { it is SseMessage.Ping }

    override fun send(message: SseMessage) = apply {
        if (dead && message is SseMessage.Ping) throw IOException("broken pipe")
        if (message is SseMessage.Event) sending(message)
        sent += message
    }

    override fun close() {
        closed = true
        closing.forEach { it() }
    }

    override fun onClose(fn: () -> Unit) = apply { closing += fn }
}

fun Page.connect(request: Request, dead: Boolean = false, sending: (SseMessage.Event) -> Unit = {}): RecordingSse {
    val stream = RecordingSse(request, dead, sending)
    sse(request).consumer(stream)
    return stream
}

class SchedulerClock(private val scheduler: DeterministicScheduler) : Clock() {
    override fun getZone(): ZoneId = UTC

    override fun withZone(zone: ZoneId) = this

    override fun instant(): Instant = scheduler.currentTime()
}

fun eventually(check: () -> Boolean) {
    val until = System.nanoTime() + SECONDS.toNanos(5)
    while (!check()) {
        check(System.nanoTime() < until) { "timed out" }
        Thread.onSpinWait()
    }
}
