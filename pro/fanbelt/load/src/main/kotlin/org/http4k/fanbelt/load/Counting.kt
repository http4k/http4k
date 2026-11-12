package org.http4k.fanbelt.load

import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.bus.Transport
import org.http4k.fanbelt.presence.PresenceBackend
import org.http4k.fanbelt.presence.PresenceMember
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.Tenants
import org.http4k.template.TemplateRenderer
import java.util.concurrent.atomic.AtomicLong

class Counts {
    val reads = AtomicLong()
    val writes = AtomicLong()
    val renders = AtomicLong()
    val messages = AtomicLong()
    val presenceWrites = AtomicLong()

    fun snapshot() = Counted(reads.get(), writes.get(), renders.get(), messages.get(), presenceWrites.get())
}

data class Counted(val reads: Long, val writes: Long, val renders: Long, val messages: Long, val presenceWrites: Long) {
    operator fun minus(before: Counted) =
        Counted(reads - before.reads, writes - before.writes, renders - before.renders, messages - before.messages, presenceWrites - before.presenceWrites)
}

class CountingStorage(private val storage: DocumentStorage, private val counts: Counts) : DocumentStorage {
    override fun <R> write(work: (Tenants) -> R): R {
        counts.writes.incrementAndGet()
        return storage.write(work)
    }

    override fun <R> read(work: (Tenants) -> R): R {
        counts.reads.incrementAndGet()
        return storage.read(work)
    }
}

class CountingTransport(private val transport: Transport, private val counts: Counts) : Transport by transport {
    override fun send(topic: Topic) {
        counts.messages.incrementAndGet()
        transport.send(topic)
    }
}

class CountingPresence(private val backend: PresenceBackend, private val counts: Counts) : PresenceBackend by backend {
    override fun set(topic: Topic, member: PresenceMember, value: Any) {
        counts.presenceWrites.incrementAndGet()
        backend.set(topic, member, value)
    }
}

fun counting(renderer: TemplateRenderer, counts: Counts): TemplateRenderer = { model ->
    counts.renders.incrementAndGet()
    renderer(model)
}
