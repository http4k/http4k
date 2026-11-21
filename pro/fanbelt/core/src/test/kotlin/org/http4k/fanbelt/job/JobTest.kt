/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.job

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.time.DeterministicScheduler
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import org.http4k.events.Event
import org.http4k.events.MetadataEvent
import org.http4k.fanbelt.SchedulerClock
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.bus.Transport
import org.http4k.fanbelt.bus.memory.InMemoryBroker
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class JobTest {
    class OrderId private constructor(value: String) : StringValue(value) {
        companion object : StringValueFactory<OrderId>(::OrderId)
    }

    private class Orders : Feature<Unit, Unit>({}) {
        val ran = CopyOnWriteArrayList<String>()
        val failing = AtomicBoolean(false)
        val duringRun = AtomicReference<() -> Unit> {}
        val order by state { _, id: OrderId -> "order $id" }
        val charge by job(order) { scope, order ->
            if (failing.get()) error("card service down")
            duringRun.getAndSet {}()
            ran += "$order in ${scope.tenant}"
        }
        val checkout by operation { _, _, ids: List<String> -> Changes.of(ids.map { charge.topic(OrderId.of(it)) }.toSet()) }
    }

    private val scheduler = DeterministicScheduler(Instant.EPOCH)
    private val storage = InMemoryDocumentStorage()
    private val infra = AppInfra(AppRuntime(clock = SchedulerClock(scheduler), scheduler = scheduler), storage).apply { start() }
    private val acme = infra.scope(TenantId.of("acme"))

    @Test
    fun `naming a key's job topic runs the job on that key's model, in that tenant`() {
        val orders = Orders()
        Jobs(infra, orders).start()

        acme.execute(orders.checkout, Unit, listOf("42"))
        scheduler.runUntilIdle()

        assertThat(orders.ran.toList(), equalTo(listOf("order 42 in acme")))
    }

    @Test
    fun `a key containing a slash runs with that whole key`() {
        val orders = Orders()
        Jobs(infra, orders).start()

        acme.execute(orders.checkout, Unit, listOf("eu/42"))
        scheduler.runUntilIdle()

        assertThat(orders.ran.toList(), equalTo(listOf("order eu/42 in acme")))
    }

    @Test
    fun `an operation can name several keys at once`() {
        val orders = Orders()
        Jobs(infra, orders).start()

        acme.execute(orders.checkout, Unit, listOf("42", "43"))
        scheduler.runUntilIdle()

        assertThat(orders.ran.toSet(), equalTo(setOf("order 42 in acme", "order 43 in acme")))
    }

    @Test
    fun `a named key is recorded, so a node that starts later runs it`() {
        val orders = Orders()
        acme.execute(orders.checkout, Unit, listOf("42"))
        scheduler.runUntilIdle()

        Jobs(infra, orders).start()
        scheduler.runUntilIdle()

        assertThat(orders.ran.toList(), equalTo(listOf("order 42 in acme")))
    }

    @Test
    fun `a run that returns is done, so the next start doesn't run it again`() {
        val orders = Orders()
        Jobs(infra, orders).start()
        acme.execute(orders.checkout, Unit, listOf("42"))
        scheduler.runUntilIdle()

        Jobs(infra, orders).start()
        scheduler.runUntilIdle()

        assertThat(orders.ran.toList(), equalTo(listOf("order 42 in acme")))
    }

    @Test
    fun `a key named again during its own run runs again straight after, so work added mid-run isn't missed`() {
        val orders = Orders()
        Jobs(infra, orders).start()
        orders.duringRun.set { acme.execute(orders.checkout, Unit, listOf("42")) }

        acme.execute(orders.checkout, Unit, listOf("42"))
        scheduler.runUntilIdle()

        assertThat(orders.ran.toList(), equalTo(listOf("order 42 in acme", "order 42 in acme")))
    }

    @Test
    fun `a key named again during its own run by a node this one doesn't hear still runs again, because its row is kept`() {
        val orders = Orders()
        Jobs(infra, orders).start()
        val unheard = AppInfra(AppRuntime(clock = SchedulerClock(scheduler), scheduler = scheduler), storage).apply { start() }
        orders.duringRun.set { unheard.scope(TenantId.of("acme")).execute(orders.checkout, Unit, listOf("42")) }

        acme.execute(orders.checkout, Unit, listOf("42"))
        scheduler.runUntilIdle()

        assertThat(orders.ran.toList(), equalTo(listOf("order 42 in acme", "order 42 in acme")))
    }

    @Test
    fun `a run that throws runs again a second later`() {
        val orders = Orders()
        Jobs(infra, orders).start()
        orders.failing.set(true)
        acme.execute(orders.checkout, Unit, listOf("42"))
        scheduler.runUntilIdle()
        orders.failing.set(false)

        scheduler.tick(Duration.ofMillis(999))
        val beforeRetry = orders.ran.toList()
        scheduler.tick(Duration.ofMillis(1))

        assertThat(beforeRetry to orders.ran.toList(), equalTo(emptyList<String>() to listOf("order 42 in acme")))
    }

    @Test
    fun `a key another node holds is skipped, and runs once that node lets go`() {
        val orders = Orders()
        Jobs(infra, orders).start()

        val whileHeld = mutableListOf<String>()
        storage.locks("@charge/42/acme") {
            acme.execute(orders.checkout, Unit, listOf("42"))
            scheduler.runUntilIdle()
            whileHeld += orders.ran
        }
        acme.execute(orders.checkout, Unit, listOf("42"))
        scheduler.runUntilIdle()

        assertThat(whileHeld to orders.ran.toList(), equalTo(emptyList<String>() to listOf("order 42 in acme")))
    }

    @Test
    fun `a key woken while another node holds it runs a second later, in case its work changed after that run began`() {
        val orders = Orders()
        Jobs(infra, orders).start()

        storage.locks("@charge/42/acme") {
            acme.execute(orders.checkout, Unit, listOf("42"))
            scheduler.runUntilIdle()
        }
        scheduler.tick(Duration.ofSeconds(1))

        assertThat(orders.ran.toList(), equalTo(listOf("order 42 in acme")))
    }

    @Test
    fun `jobs run on a worker node of their own, named by an operation on a node that runs none`() {
        val broker = InMemoryBroker()
        val storage = InMemoryDocumentStorage()
        val web = AppInfra(AppRuntime(clock = SchedulerClock(scheduler), scheduler = scheduler), storage, broker.transport()).apply { start() }
        val worker = AppInfra(AppRuntime(clock = SchedulerClock(scheduler), scheduler = scheduler), storage, broker.transport()).apply { start() }
        val orders = Orders()
        Jobs(worker, orders).start()

        web.scope(TenantId.of("acme")).execute(orders.checkout, Unit, listOf("42"))
        scheduler.runUntilIdle()

        assertThat(orders.ran.toList(), equalTo(listOf("order 42 in acme")))
    }

    @Test
    fun `a run that throws is reported as the job failing`() {
        val events = CopyOnWriteArrayList<Event>()
        val infra = AppInfra(AppRuntime(clock = SchedulerClock(scheduler), scheduler = scheduler, events = { events += if (it is MetadataEvent) it.event else it })).apply { start() }
        val orders = Orders()
        Jobs(infra, orders).start()
        orders.failing.set(true)

        infra.scope(TenantId.of("acme")).execute(orders.checkout, Unit, listOf("42"))
        scheduler.runUntilIdle()

        assertThat(events.filterIsInstance<JobFailed>().map { it.job }, equalTo(listOf("charge")))
    }

    @Test
    fun `a closed runner runs nothing more, neither named nor retried`() {
        val orders = Orders()
        val jobs = Jobs(infra, orders).apply { start() }
        orders.failing.set(true)
        acme.execute(orders.checkout, Unit, listOf("42"))
        scheduler.runUntilIdle()
        orders.failing.set(false)

        jobs.close()
        acme.execute(orders.checkout, Unit, listOf("43"))
        scheduler.tick(Duration.ofSeconds(5))

        assertThat(orders.ran.toList(), equalTo(emptyList()))
    }

    @Test
    fun `a runner looks again at recorded work when the backplane reconnects, because a wake may have been missed`() {
        val reconnect = CopyOnWriteArrayList<() -> Unit>()
        val transport = object : Transport {
            override fun send(topic: Topic) {}
            override fun start(received: (Topic) -> Unit, reconnected: () -> Unit) {
                reconnect += reconnected
            }
            override fun close() {}
        }
        val storage = InMemoryDocumentStorage()
        val worker = AppInfra(AppRuntime(clock = SchedulerClock(scheduler), scheduler = scheduler, events = {}), storage, transport).apply { start() }
        val web = AppInfra(AppRuntime(clock = SchedulerClock(scheduler), scheduler = scheduler, events = {}), storage).apply { start() }
        val orders = Orders()
        Jobs(worker, orders).start()

        web.scope(TenantId.of("acme")).execute(orders.checkout, Unit, listOf("42"))
        scheduler.runUntilIdle()
        val missed = orders.ran.toList()
        reconnect.single()()
        scheduler.runUntilIdle()

        assertThat(missed to orders.ran.toList(), equalTo(emptyList<String>() to listOf("order 42 in acme")))
    }

    private class Refunds : Feature<Unit, Unit>({}) {
        val order by state { _, id: OrderId -> "order $id" }
        val charge by job(order) { _, _ -> }
    }

    @Test
    fun `two jobs with the same name can't run together, because each would run the other's work`() {
        val failure = runCatching { Jobs(infra, Orders(), Refunds()) }.exceptionOrNull()

        assertThat(failure?.message, equalTo("Jobs share a name, so would run each other's work: charge"))
    }
}
