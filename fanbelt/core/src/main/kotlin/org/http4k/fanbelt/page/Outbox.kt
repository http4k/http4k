package org.http4k.fanbelt.page

import org.http4k.datastar.Element
import org.http4k.datastar.MorphMode.append
import org.http4k.datastar.Selector
import org.http4k.events.Events
import org.http4k.fanbelt.bus.Subscriber
import org.http4k.fanbelt.events.Patched
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage
import org.http4k.sse.sendPatchElements
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal class Outbox(
    private val sse: Sse,
    sending: Executor,
    private val events: Events,
    painted: Map<RegionId, String>,
    private val actor: Any?,
) {
    private val latest = ConcurrentHashMap<RegionId, Rendered>()
    private val sent = ConcurrentHashMap(painted)
    private val pingDue = AtomicBoolean(false)
    private val leaving = AtomicReference<Element>()
    private val draining = Subscriber(sending, events, ::drain)

    fun post(region: RegionId, rendered: Rendered) {
        latest[region] = rendered
        draining.markDirty()
    }

    fun ping() {
        pingDue.set(true)
        draining.markDirty()
    }

    fun leave(script: Element) {
        leaving.set(script)
        draining.markDirty()
    }

    private fun drain() {
        leaving.getAndSet(null)?.also { script ->
            write { sse.sendPatchElements(script, morphMode = append, selector = BODY) }
            return sse.close()
        }
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

private val BODY = Selector.of("body")
