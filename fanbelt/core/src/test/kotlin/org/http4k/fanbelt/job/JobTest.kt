package org.http4k.fanbelt.job

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import dev.forkhandles.time.DeterministicScheduler
import dev.forkhandles.values.Maskers.hidden
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.http4k.events.Event
import org.http4k.events.MetadataEvent
import org.http4k.fanbelt.bus.SubscriberFailed
import org.http4k.fanbelt.bus.memory.InMemoryBroker
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.Tenants
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.junit.jupiter.api.Test

class JobTest {
    class OrderId private constructor(value: String) : StringValue(value) {
        companion object : StringValueFactory<OrderId>(::OrderId)
    }

    private class Orders : Feature<Unit, Unit>({}) {
        val ran = CopyOnWriteArrayList<String>()
        val failing = AtomicBoolean(false)
        val duringRun = AtomicReference {}
        val charge by job(OrderId) { scope, id ->
            if (failing.get()) error("card service down")
            duringRun.getAndSet {}()
            ran += "order $id in ${scope.tenant}"
        }
        val checkout by operation { _, _, ids: List<String> -> Changes.of(ids.map { charge.topic(OrderId.of(it)) }.toSet()) }
        val shipped = CopyOnWriteArrayList<String>()
        val ship by job(OrderId) { _, id -> shipped += "order $id" }
        val dispatch by operation { _, _, id: String -> Changes.of(charge.topic(OrderId.of(id)), ship.topic(OrderId.of(id))) }
    }

    private val scheduler = DeterministicScheduler(Instant.EPOCH)
    private val storage = InMemoryDocumentStorage()
    private val infra = AppInfra(AppRuntime(clock = scheduler.toClock(), scheduler = scheduler), storage).apply { start() }
    private val acme = infra.scope(TenantId.of("acme"))

    @Test
    fun `a node runs only the jobs it is given, so a job runner can take some of a feature's jobs`() {
        val orders = Orders()
        Jobs(infra, orders.charge).start()

        acme.execute(orders.dispatch, "42")
        scheduler.runUntilIdle()

        assertThat(orders.ran.toList() to orders.shipped.toList(), equalTo(listOf("order 42 in acme") to emptyList()))
    }

    private class RenamedOrders : Feature<Unit, Unit>({}) {
        val ran = CopyOnWriteArrayList<String>()
        val chargeCard by job(OrderId, name = JobName.of("charge")) { _, id -> ran += "order $id" }
    }

    @Test
    fun `a job keeps the name it is given when its property is renamed, so work recorded before still runs`() {
        acme.execute(Orders().checkout, listOf("42"))
        scheduler.runUntilIdle()

        val renamed = RenamedOrders()
        Jobs(infra, renamed.chargeCard).start()
        scheduler.runUntilIdle()

        assertThat(renamed.ran.toList(), equalTo(listOf("order 42")))
    }

    @Test
    fun `a job's name can't hold the characters its recorded rows are split on`() {
        assertThat({ JobName.of("charge/eu") }, throws<IllegalArgumentException>())
        assertThat({ JobName.of("@charge") }, throws<IllegalArgumentException>())
    }

    @Test
    fun `naming a key's job topic runs the job on that key, in that tenant`() {
        val orders = Orders()
        Jobs(infra, orders.charge).start()

        acme.execute(orders.checkout, listOf("42"))
        scheduler.runUntilIdle()

        assertThat(orders.ran.toList(), equalTo(listOf("order 42 in acme")))
    }

    private class CardToken private constructor(value: String) : StringValue(value, hidden()) {
        companion object : StringValueFactory<CardToken>(::CardToken)
    }

    private class Cards : Feature<Unit, Unit>({}) {
        val charged = CopyOnWriteArrayList<String>()
        val charge by job(CardToken) { _, token -> charged += token.value }
        val pay by operation { _, _, token: String -> Changes.of(charge.topic(CardToken.of(token))) }
    }

    @Test
    fun `a key that prints masked still runs with its real value`() {
        val cards = Cards()
        Jobs(infra, cards.charge).start()

        acme.execute(cards.pay, "tok_4242")
        scheduler.runUntilIdle()

        assertThat(cards.charged.toList(), equalTo(listOf("tok_4242")))
    }

    @Test
    fun `the same key named in two tenants runs once in each`() {
        val orders = Orders()
        Jobs(infra, orders.charge).start()

        acme.execute(orders.checkout, listOf("42"))
        infra.scope(TenantId.of("globex")).execute(orders.checkout, listOf("42"))
        scheduler.runUntilIdle()

        assertThat(orders.ran.sorted(), equalTo(listOf("order 42 in acme", "order 42 in globex")))
    }

    @Test
    fun `a key containing a slash runs with that whole key`() {
        val orders = Orders()
        Jobs(infra, orders.charge).start()

        acme.execute(orders.checkout, listOf("eu/42"))
        scheduler.runUntilIdle()

        assertThat(orders.ran.toList(), equalTo(listOf("order eu/42 in acme")))
    }

    @Test
    fun `an operation can name several keys at once`() {
        val orders = Orders()
        Jobs(infra, orders.charge).start()

        acme.execute(orders.checkout, listOf("42", "43"))
        scheduler.runUntilIdle()

        assertThat(orders.ran.toSet(), equalTo(setOf("order 42 in acme", "order 43 in acme")))
    }

    @Test
    fun `a named key is recorded, so a node that starts later runs it`() {
        val orders = Orders()
        acme.execute(orders.checkout, listOf("42"))
        scheduler.runUntilIdle()

        Jobs(infra, orders.charge).start()
        scheduler.runUntilIdle()

        assertThat(orders.ran.toList(), equalTo(listOf("order 42 in acme")))
    }

    @Test
    fun `a run that returns is done, so the next start doesn't run it again`() {
        val orders = Orders()
        Jobs(infra, orders.charge).start()
        acme.execute(orders.checkout, listOf("42"))
        scheduler.runUntilIdle()

        Jobs(infra, orders.charge).start()
        scheduler.runUntilIdle()

        assertThat(orders.ran.toList(), equalTo(listOf("order 42 in acme")))
    }

    @Test
    fun `a key named again during its own run runs again straight after, so work added mid-run isn't missed`() {
        val orders = Orders()
        Jobs(infra, orders.charge).start()
        orders.duringRun.set { acme.execute(orders.checkout, listOf("42")) }

        acme.execute(orders.checkout, listOf("42"))
        scheduler.runUntilIdle()

        assertThat(orders.ran.toList(), equalTo(listOf("order 42 in acme", "order 42 in acme")))
    }

    @Test
    fun `a key named again during its own run by a node this one doesn't hear still runs again, because its row is kept`() {
        val orders = Orders()
        Jobs(infra, orders.charge).start()
        val unheard = AppInfra(AppRuntime(clock = scheduler.toClock(), scheduler = scheduler), storage).apply { start() }
        orders.duringRun.set { unheard.scope(TenantId.of("acme")).execute(orders.checkout, listOf("42")) }

        acme.execute(orders.checkout, listOf("42"))
        scheduler.runUntilIdle()

        assertThat(orders.ran.toList(), equalTo(listOf("order 42 in acme", "order 42 in acme")))
    }

    @Test
    fun `a run that throws runs again a second later`() {
        val orders = Orders()
        Jobs(infra, orders.charge).start()
        orders.failing.set(true)
        acme.execute(orders.checkout, listOf("42"))
        scheduler.runUntilIdle()
        orders.failing.set(false)

        scheduler.tick(Duration.ofMillis(999))
        val beforeRetry = orders.ran.toList()
        scheduler.tick(Duration.ofMillis(1))

        assertThat(beforeRetry to orders.ran.toList(), equalTo(emptyList<String>() to listOf("order 42 in acme")))
    }

    @Test
    fun `a failing key named twice still retries once a second, not once per naming`() {
        val events = CopyOnWriteArrayList<Event>()
        val infra = AppInfra(AppRuntime(clock = scheduler.toClock(), scheduler = scheduler, events = { events += if (it is MetadataEvent) it.event else it })).apply { start() }
        val orders = Orders()
        Jobs(infra, orders.charge).start()
        orders.failing.set(true)

        infra.scope(TenantId.of("acme")).execute(orders.checkout, listOf("42"))
        infra.scope(TenantId.of("acme")).execute(orders.checkout, listOf("42"))
        scheduler.tick(Duration.ofSeconds(1))

        assertThat(events.filterIsInstance<JobFailed>().size, equalTo(2))
    }

    @Test
    fun `a key another node holds is skipped, and runs once that node lets go`() {
        val orders = Orders()
        Jobs(infra, orders.charge).start()

        val whileHeld = mutableListOf<String>()
        storage.locks("@charge/42/acme") {
            acme.execute(orders.checkout, listOf("42"))
            scheduler.runUntilIdle()
            whileHeld += orders.ran
        }
        acme.execute(orders.checkout, listOf("42"))
        scheduler.tick(Duration.ofSeconds(1))

        assertThat(whileHeld to orders.ran.toList(), equalTo(emptyList<String>() to listOf("order 42 in acme")))
    }

    @Test
    fun `a key is run once however many nodes run jobs, because a node that finds its work done skips it`() {
        val broker = InMemoryBroker()
        val storage = InMemoryDocumentStorage()
        val nodes = List(3) { AppInfra(AppRuntime(clock = scheduler.toClock(), scheduler = scheduler), storage, broker.transport()).apply { start() } }
        val orders = Orders()
        nodes.forEach { Jobs(it, orders.charge).start() }

        nodes.first().scope(TenantId.of("acme")).execute(orders.checkout, listOf("42"))
        scheduler.runUntilIdle()
        scheduler.tick(Duration.ofSeconds(1))

        assertThat(orders.ran.toList(), equalTo(listOf("order 42 in acme")))
    }

    @Test
    fun `jobs run on a worker node of their own, named by an operation on a node that runs none`() {
        val broker = InMemoryBroker()
        val storage = InMemoryDocumentStorage()
        val web = AppInfra(AppRuntime(clock = scheduler.toClock(), scheduler = scheduler), storage, broker.transport()).apply { start() }
        val worker = AppInfra(AppRuntime(clock = scheduler.toClock(), scheduler = scheduler), storage, broker.transport()).apply { start() }
        val orders = Orders()
        Jobs(worker, orders.charge).start()

        web.scope(TenantId.of("acme")).execute(orders.checkout, listOf("42"))
        scheduler.runUntilIdle()

        assertThat(orders.ran.toList(), equalTo(listOf("order 42 in acme")))
    }

    @Test
    fun `a run that throws is reported as the job failing`() {
        val events = CopyOnWriteArrayList<Event>()
        val infra = AppInfra(AppRuntime(clock = scheduler.toClock(), scheduler = scheduler, events = { events += if (it is MetadataEvent) it.event else it })).apply { start() }
        val orders = Orders()
        Jobs(infra, orders.charge).start()
        orders.failing.set(true)

        infra.scope(TenantId.of("acme")).execute(orders.checkout, listOf("42"))
        scheduler.runUntilIdle()

        assertThat(events.filterIsInstance<JobFailed>().map { it.job }, equalTo(listOf("charge")))
    }

    @Test
    fun `a wake whose owing tenants can't be read is reported, not lost silently`() {
        val events = CopyOnWriteArrayList<Event>()
        val readFails = AtomicBoolean(false)
        val flaky = object : DocumentStorage by storage {
            override fun <R> read(work: (Tenants) -> R): R = if (readFails.get()) error("database down") else storage.read(work)
        }
        val infra = AppInfra(AppRuntime(clock = scheduler.toClock(), scheduler = scheduler, events = { events += if (it is MetadataEvent) it.event else it }), flaky).apply { start() }
        val orders = Orders()
        Jobs(infra, orders.charge).start()

        infra.scope(TenantId.of("acme")).execute(orders.checkout, listOf("42"))
        readFails.set(true)
        scheduler.runUntilIdle()

        assertThat(events.filterIsInstance<SubscriberFailed>().map { it.cause.message }, equalTo(listOf("database down")))
    }

    @Test
    fun `a closed runner runs nothing more, neither named nor retried`() {
        val orders = Orders()
        val jobs = Jobs(infra, orders.charge).apply { start() }
        orders.failing.set(true)
        acme.execute(orders.checkout, listOf("42"))
        scheduler.runUntilIdle()
        orders.failing.set(false)

        jobs.close()
        acme.execute(orders.checkout, listOf("43"))
        scheduler.tick(Duration.ofSeconds(5))

        assertThat(orders.ran.toList(), equalTo(emptyList()))
    }

    @Test
    fun `a runner looks again at recorded work every heal, so a wake it never heard is run without a restart or reconnect`() {
        val storage = InMemoryDocumentStorage()
        val worker = AppInfra(AppRuntime(clock = scheduler.toClock(), scheduler = scheduler, events = {}), storage).apply { start() }
        val web = AppInfra(AppRuntime(clock = scheduler.toClock(), scheduler = scheduler, events = {}), storage).apply { start() }
        val orders = Orders()
        Jobs(worker, orders.charge).start()

        web.scope(TenantId.of("acme")).execute(orders.checkout, listOf("42"))
        scheduler.runUntilIdle()
        val missed = orders.ran.toList()
        scheduler.tick(Duration.ofMinutes(5))

        assertThat(missed to orders.ran.toList(), equalTo(emptyList<String>() to listOf("order 42 in acme")))
    }

    private class Refunds : Feature<Unit, Unit>({}) {
        val charge by job(OrderId) { _, _ -> }
    }

    @Test
    fun `two jobs with the same name can't run together, because each would run the other's work`() {
        val failure = runCatching { Jobs(infra, Orders().charge, Refunds().charge) }.exceptionOrNull()

        assertThat(failure?.message, equalTo("Jobs share a name, so would run each other's work: charge"))
    }
}
