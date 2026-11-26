package org.http4k.fanbelt.presence

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.time.DeterministicScheduler
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.fanbelt.acme
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.page.Region
import org.http4k.fanbelt.state.DerivedState
import org.http4k.fanbelt.state.reading
import org.http4k.template.ViewModel
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class PresenceReadsTest {
    private class Who private constructor(value: String) : StringValue(value) {
        companion object : NonBlankStringValueFactory<Who>(::Who)
    }

    private object Room : Feature<Unit, Unit>({}) {
        val here by presence(Who)
        val inRoom by presence(Who, Who)
    }

    private val reading = AtomicBoolean(false)
    private val asked = AtomicInteger()
    private val askedWhileReading = AtomicInteger()
    private val reads = AtomicInteger()

    private val presence = object : PresenceBackend {
        override fun present(topic: Topic): Map<PresenceMember, String?> {
            ask()
            return mapOf(PresenceMember.of("alex") to null)
        }

        override fun present(topics: Set<Topic>): Map<Topic, Map<PresenceMember, String?>> {
            ask()
            return topics.associateWith { mapOf(PresenceMember.of("alex") to null) }
        }

        private fun ask() {
            asked.incrementAndGet()
            if (reading.get()) askedWhileReading.incrementAndGet()
        }

        override fun join(connections: List<PresenceConnection>, ttl: Duration) {}
        override fun leave(connection: PresenceConnection) {}
        override fun set(topic: Topic, member: PresenceMember, value: String) {}
        override fun sweep() = emptySet<Topic>()
    }

    private val scheduler = DeterministicScheduler(Instant.EPOCH)
    private val scope = AppInfra(AppRuntime(scheduler = scheduler), presenceBackend = presence).apply { start() }.scope(acme)

    private val viewers = DerivedState<Unit, Set<String>>("viewers") { ctx, _ ->
        reads.incrementAndGet()
        reading.set(true)
        try {
            ctx.read(Room.here).keys.map { it.value }.toSet()
        } finally {
            reading.set(false)
        }
    }

    @Test
    fun `presence is read outside the storage transaction, and the read sees it`() {
        assertThat(scope.reading { ctx -> ctx.read(viewers) }.value, equalTo(setOf("alex")))
        assertThat(askedWhileReading.get(), equalTo(0))
    }

    @Test
    fun `a read of many presence topics asks the backend once`() {
        val seen = scope.reading { ctx -> listOf("a", "b", "c").map { ctx.read(Room.inRoom, Who.of(it)).size } }

        assertThat(seen.value, equalTo(listOf(1, 1, 1)))
        assertThat(asked.get(), equalTo(1))
    }

    @Test
    fun `a region remembers which presence it read, so later redraws read once`() {
        val region = Region("viewers", { """<p id="viewers">$it</p>""" }, viewers) { names -> Names(names) }
        region.watch(scope, Request(GET, "/"), Unit) {}
        reads.set(0)

        scope.bus.publish(Topic.of("here"))
        scheduler.runUntilIdle()

        assertThat(reads.get(), equalTo(1))
        assertThat(askedWhileReading.get(), equalTo(0))
    }

    private data class Names(val names: Set<String>) : ViewModel
}
