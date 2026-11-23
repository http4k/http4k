package org.http4k.fanbelt.page

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.fanbelt.SchedulerClock
import org.http4k.fanbelt.acme
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.state.DerivedState
import org.http4k.fanbelt.state.StateContext
import org.http4k.template.ViewModel
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class RegionWatchTest {
    private data class Shown(val value: String) : ViewModel

    private val scheduler = DeterministicScheduler(Instant.parse("2026-01-01T09:00:00Z"))
    private val clock = SchedulerClock(scheduler)
    private val infra = AppInfra(AppRuntime(clock = clock, scheduler = scheduler, events = {})).apply { start() }
    private val scope = infra.scope(acme)
    private val reads = AtomicInteger()

    private fun region(name: String, read: (StateContext<Unit>, Unit) -> String) =
        Region(name, { """<p id="$name">${(it as Shown).value}</p>""" }, DerivedState(name, read)) { value, _ -> Shown(value) }

    @Test
    fun `a region redraws for its watchers in the order they arrived, so a run can be replayed`() {
        var value = 1
        val region = region("n") { ctx, _ -> ctx.watch(Topic.of("t")); "$value" }
        val heard = mutableListOf<Int>()
        (1..50).forEach { watcher -> region.watch(scope, Request(GET, "/")) { if (it.html.contains(">2<")) heard += watcher } }
        scheduler.runUntilIdle()

        value = 2
        scope.bus.publish(Topic.of("t"))
        scheduler.runUntilIdle()

        assertThat(heard, equalTo((1..50).toList()))
    }

    @Test
    fun `a redraw already queued when the last viewer leaves doesn't leave a subscription behind`() {
        val before = Topic.of("before")
        val after = Topic.of("after")
        val moved = AtomicBoolean(false)
        val region = region("watched") { ctx, _ ->
            reads.incrementAndGet()
            ctx.watch(if (moved.get()) after else before)
            "v${reads.get()}"
        }
        val watching = region.watch(scope, Request(GET, "/")) {}

        moved.set(true)
        infra.bus.publish(scope.tenant(before))
        watching.close()
        scheduler.runUntilIdle()
        val afterLeaving = reads.get()

        infra.bus.publish(scope.tenant(after))
        scheduler.runUntilIdle()

        assertThat(reads.get(), equalTo(afterLeaving))
    }

    private val closesAt = clock.instant() + Duration.ofSeconds(5)

    private val lot = region("lot") { ctx, _ ->
        reads.incrementAndGet()
        if (clock.instant() < closesAt) {
            ctx.stale(at = closesAt)
            "open"
        } else {
            "closed"
        }
    }

    @Test
    fun `a read that says when it goes stale is read again then, with nothing published`() {
        val sent = CopyOnWriteArrayList<String>()
        lot.watch(scope, Request(GET, "/")) { sent += it.html }

        scheduler.tick(Duration.ofSeconds(4))
        assertThat(sent, equalTo(listOf("""<p id="lot">open</p>""")))

        scheduler.tick(Duration.ofSeconds(1))
        assertThat(sent.last(), equalTo("""<p id="lot">closed</p>"""))
    }

    @Test
    fun `a read that says it went stale in the past isn't read again for it`() {
        val expiring = region("token") { ctx, _ ->
            if (reads.incrementAndGet() < 10) ctx.stale(at = closesAt)
            "seen"
        }
        expiring.watch(scope, Request(GET, "/")) {}

        scheduler.tick(Duration.ofSeconds(6))

        assertThat(reads.get(), equalTo(2))
    }

    @Test
    fun `a read that went stale in the past still goes stale at a later time it gives`() {
        val both = region("both") { ctx, _ ->
            reads.incrementAndGet()
            ctx.stale(at = clock.instant() - Duration.ofSeconds(1))
            ctx.stale(at = closesAt)
            "seen"
        }
        both.watch(scope, Request(GET, "/")) {}

        scheduler.tick(Duration.ofSeconds(5))

        assertThat(reads.get(), equalTo(2))
    }

    @Test
    fun `a region nobody watches isn't read when its read goes stale`() {
        lot.watch(scope, Request(GET, "/")) {}.close()
        val afterLeaving = reads.get()

        scheduler.tick(Duration.ofSeconds(5))

        assertThat(reads.get(), equalTo(afterLeaving))
    }
}
