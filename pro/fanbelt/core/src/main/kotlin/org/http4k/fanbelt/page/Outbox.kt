/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import org.http4k.datastar.Element
import org.http4k.events.Events
import org.http4k.fanbelt.bus.Subscriber
import org.http4k.fanbelt.events.Patched
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage
import org.http4k.sse.sendPatchElements
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

internal class Outbox(
    private val sse: Sse,
    sending: Executor,
    private val events: Events,
    painted: Map<RegionId, String>,
    private val actor: String?,
) {
    private val latest = ConcurrentHashMap<RegionId, Rendered>()
    private val sent = ConcurrentHashMap(painted)
    private val pingDue = AtomicBoolean(false)
    private val draining = Subscriber(sending, events, ::drain)

    fun post(region: RegionId, rendered: Rendered) {
        latest[region] = rendered
        draining.markDirty()
    }

    fun ping() {
        pingDue.set(true)
        draining.markDirty()
    }

    private fun drain() {
        latest.keys.forEach { region -> latest.remove(region)?.also { send(region, it) } }
        if (pingDue.getAndSet(false)) write { sse.send(SseMessage.Ping) }
    }

    private fun send(region: RegionId, rendered: Rendered) {
        val changed = sent.put(region, rendered.digest) != rendered.digest
        events(Patched(region, actor, changed))
        if (changed) {
            write { sse.sendPatchElements(Element.of(rendered.html)) }
        }
    }

    private fun write(message: () -> Unit) {
        runCatching(message).onFailure { sse.close() }
    }
}
