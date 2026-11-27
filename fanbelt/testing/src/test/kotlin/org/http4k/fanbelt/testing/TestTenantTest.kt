package org.http4k.fanbelt.testing

import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Hidden
import org.http4k.fanbelt.feature.Allowed
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.feature.Outcome
import org.http4k.fanbelt.input.FieldName
import org.http4k.fanbelt.state.StateContext
import org.http4k.fanbelt.state.join
import org.junit.jupiter.api.Test

class TestTenantTest {
    private val tenant = TestTenant()
    private val closesAt = tenant.clock.instant() + Duration.ofMinutes(10)

    @Test
    fun `a read that changes once its stale time has passed isn't a missed publish`() {
        val lot = Lot { ctx ->
            if (tenant.clock.instant() < closesAt) "open".also { ctx.stale(at = closesAt) } else "closed"
        }.lot
        tenant {
            lot.read()

            tick(Duration.ofMinutes(11))

            assertThat(lot.read(), equalTo("closed"))
        }
    }

    @Test
    fun `a read that changes before its stale time without a publish is still caught`() {
        val value = AtomicReference("open")
        val lot = Lot { ctx -> value.get().also { ctx.stale(at = closesAt) } }.lot
        tenant {
            lot.read()

            value.set("closed")

            assertThat({ tick(Duration.ofMinutes(1)) }, throws<IllegalStateException>())
        }
    }

    @Test
    fun `a publish of a topic the state no longer reads doesn't excuse a change`() {
        val value = AtomicReference("open")
        val reads = AtomicReference(Topic.of("a"))
        val lot = Lot { ctx -> ctx.watch(reads.get()); value.get() }.lot
        tenant {
            lot.read()
            reads.set(Topic.of("b"))
            lot.read()

            value.set("closed")
            scope.bus.publish(setOf(Topic.of("a")))

            assertThat({ settle() }, throws<IllegalStateException>())
        }
    }

    @Test
    fun `an operation that changes a read state without publishing fails, though the test never reads it again`() {
        val value = AtomicReference("open")
        val auction = Auction(value, publishes = false)
        tenant {
            auction.lot.read()

            assertThat({ auction.close.execute(Unit) }, throws<IllegalStateException>())
        }
    }

    @Test
    fun `an operation that publishes what it changed passes, though the test never reads it again`() {
        val value = AtomicReference("open")
        val auction = Auction(value, publishes = true)
        tenant {
            auction.lot.read()

            assertThat(auction.close.execute(Unit), equalTo(Verdict.Accepted))
        }
    }

    @Test
    fun `a given operation hands back its result`() {
        tenant {
            assertThat(Cloakroom.hang.given("scarf"), equalTo("ticket for scarf"))
        }
    }

    @Test
    fun `a given operation that is refused fails the step`() {
        tenant {
            assertThat({ Cloakroom.hang.given(" ") }, throws<IllegalStateException>())
        }
    }

    @Test
    fun `a read straight after a published change made outside the kit sees it`() {
        val auction = Auction(AtomicReference("open"), publishes = true)
        tenant {
            auction.lot.read()

            scope.execute(auction.close, Unit, Unit)

            assertThat(auction.lot.read(), equalTo("closed"))
        }
    }

    @Test
    fun `inside the tenant, a feature's operations and states are called directly`() {
        val auction = Auction(AtomicReference("open"), publishes = true)

        tenant {
            assertThat(auction.lot.read(), equalTo("open"))
            assertThat(auction.close.execute(Unit), equalTo(Verdict.Accepted))
            assertThat(auction.lot.read(), equalTo("closed"))
        }
    }

    @Test
    fun `inside the tenant, a rule is read for an actor and a key, or an actor alone`() {
        tenant {
            assertThat(Door.opens.read("bob", "vault"), equalTo(Hidden))
            assertThat(Door.opens.read("bob", "hall"), equalTo(Allowed("hall")))
            assertThat(Door.staff.read("ann"), equalTo(Allowed("ann")))
        }
    }

    @Test
    fun `a visit opened inside the tenant has arrived when it returns, and has left when it closes`() {
        val ann = OrderId.of("ann")

        tenant {
            val visit = Lobby.arrive.open(ann)
            assertThat(Lobby.here.read().keys, equalTo(setOf(ann)))

            visit.close()
            assertThat(Lobby.here.read().keys, equalTo(emptySet()))
        }
    }

    private object Door : Feature<Unit, String>({}) {
        val opens by rule { _, who, room: String -> if (who == "ann" || room == "hall") Allowed(room) else Hidden }
        val staff by rule { _, who -> if (who == "ann") Allowed(who) else Denied }
    }

    private object Lobby : Feature<Unit, OrderId>({}) {
        val here by presence(OrderId)
        val arrive by visit { scope, who -> here.join(scope, who) }
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

        tenant {
            payments.checkout.execute(Unit)

            assertThat(payments.charged, equalTo(listOf("order 42")))
        }
    }
}
