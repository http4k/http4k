package org.http4k.fanbelt.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.input.FieldName
import org.http4k.fanbelt.feature.Outcome
import org.http4k.fanbelt.state.StateContext
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

class TestTenantTest {
    private val tenant = TestTenant()
    private val closesAt = tenant.clock.instant() + Duration.ofMinutes(10)

    @Test
    fun `a read that changes once its stale time has passed isn't a missed publish`() {
        val lot = Lot { ctx ->
            if (tenant.clock.instant() < closesAt) "open".also { ctx.stale(at = closesAt) } else "closed"
        }.lot
        tenant.read(lot)

        tenant.tick(Duration.ofMinutes(11))

        assertThat(tenant.read(lot), equalTo("closed"))
    }

    @Test
    fun `a read that changes before its stale time without a publish is still caught`() {
        val value = AtomicReference("open")
        val lot = Lot { ctx -> value.get().also { ctx.stale(at = closesAt) } }.lot
        tenant.read(lot)

        value.set("closed")

        assertThat({ tenant.tick(Duration.ofMinutes(1)) }, throws<IllegalStateException>())
    }

    @Test
    fun `a publish of a topic the state no longer reads doesn't excuse a change`() {
        val value = AtomicReference("open")
        val reads = AtomicReference(Topic.of("a"))
        val lot = Lot { ctx -> ctx.watch(reads.get()); value.get() }.lot
        tenant.read(lot)
        reads.set(Topic.of("b"))
        tenant.read(lot)

        value.set("closed")
        tenant.scope.bus.publish(setOf(Topic.of("a")))

        assertThat({ tenant.settle() }, throws<IllegalStateException>())
    }

    @Test
    fun `an operation that changes a read state without publishing fails, though the test never reads it again`() {
        val value = AtomicReference("open")
        val auction = Auction(value, publishes = false)
        tenant.read(auction.lot)

        assertThat({ tenant.execute(auction.close, Unit) }, throws<IllegalStateException>())
    }

    @Test
    fun `an operation that publishes what it changed passes, though the test never reads it again`() {
        val value = AtomicReference("open")
        val auction = Auction(value, publishes = true)
        tenant.read(auction.lot)

        assertThat(tenant.execute(auction.close, Unit), equalTo(Verdict.Accepted))
    }

    @Test
    fun `a given operation hands back its result`() {
        assertThat(tenant.given(Cloakroom.hang, "scarf"), equalTo("ticket for scarf"))
    }

    @Test
    fun `a given operation that is refused fails the step`() {
        assertThat({ tenant.given(Cloakroom.hang, " ") }, throws<IllegalStateException>())
    }

    @Test
    fun `a read straight after a published change made outside the kit sees it`() {
        val auction = Auction(AtomicReference("open"), publishes = true)
        tenant.read(auction.lot)

        tenant.scope.execute(auction.close, Unit, Unit)

        assertThat(tenant.read(auction.lot), equalTo("closed"))
    }

    private object Cloakroom : Feature<Unit, Unit>({}) {
        val hang by operation { _, _, coat: String ->
            if (coat.isBlank()) Outcome.Rejected(FieldName.of("coat"), "Hand over a coat") else Changes.of(emptySet(), "ticket for $coat")
        }
    }

    private class Auction(value: AtomicReference<String>, publishes: Boolean) : Feature<Unit, Unit>({}) {
        val lot by derived { ctx, _: Unit -> ctx.watch(LOT); value.get() }
        val close by operation { _, _, _: Unit ->
            value.set("closed")
            if (publishes) Changes.of(LOT) else Changes.none()
        }
    }

    private class Lot(read: (StateContext<Unit>) -> String) : Feature<Unit, Unit>({}) {
        val lot by derived { ctx, _: Unit -> read(ctx) }
    }

    private companion object {
        val LOT = Topic.of("lot")
    }

    class OrderId private constructor(value: String) : StringValue(value) {
        companion object : StringValueFactory<OrderId>(::OrderId)
    }

    private class Payments : Feature<Unit, Unit>({}) {
        val charged = CopyOnWriteArrayList<String>()
        val charge by job(OrderId) { _, id -> charged += "order $id" }
        val checkout by operation { _, _, _: Unit -> Changes.of(charge.topic(OrderId.of("42"))) }
    }

    @Test
    fun `a job the tenant runs has run by the time the operation that woke it returns`() {
        val payments = Payments()
        tenant.runs(payments.charge)

        tenant.execute(payments.checkout, Unit)

        assertThat(payments.charged, equalTo(listOf("order 42")))
    }
}
