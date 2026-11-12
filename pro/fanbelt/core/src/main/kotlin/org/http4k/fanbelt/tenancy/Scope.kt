/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.tenancy

import org.http4k.events.Events
import org.http4k.fanbelt.bus.Bus
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.events.Published
import org.http4k.fanbelt.events.eventTenant
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.presence.Presence
import org.http4k.fanbelt.presence.PresenceMember
import org.http4k.fanbelt.state.State
import org.http4k.fanbelt.state.reading
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.Tables
import java.time.Duration
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit.NANOSECONDS
import kotlin.reflect.KClass

class Scope internal constructor(
    val tenant: TenantId,
    private val storage: DocumentStorage,
    shared: Bus,
    presence: Presence,
    private val redrawing: Semaphore,
    internal val runtime: AppRuntime,
) {
    internal val events: Events = { runtime.events(eventTenant(tenant, it)) }

    val bus: Bus = TenantBus(shared, tenant, events)

    fun <Domain, R> read(feature: Feature<Domain, *>, block: (Domain) -> R): R =
        tables { tables -> block(feature.domain(tables)) }

    fun <Key : Any, Model> read(state: State<Key, Model>, key: Key): Model = reading { ctx -> ctx.read(state, key) }.value

    fun <Model> read(state: State<Unit, Model>): Model = read(state, Unit)

    internal val presence: Presence = TenantPresence(presence, tenant)

    internal fun <R> changing(block: (Tables) -> R): R = storage.write { block(it.tables(tenant)) }

    internal fun <R> redraw(waited: (Duration) -> Unit, block: () -> R): R {
        if (!redrawing.tryAcquire(0, NANOSECONDS)) {
            val started = System.nanoTime()
            redrawing.acquire()
            waited(Duration.ofNanos(System.nanoTime() - started))
        }
        try {
            return block()
        } finally {
            redrawing.release()
        }
    }

    internal fun <R> tables(block: (Tables) -> R): R = storage.read { block(it.tables(tenant)) }
}

private class TenantBus(private val shared: Bus, private val tenant: TenantId, private val events: Events) : Bus {
    override fun publish(topics: Set<Topic>) {
        topics.forEach { events(Published(it)) }
        shared.publish(topics.map(tenant::invoke).toSet())
    }

    override fun subscribe(topics: Set<Topic>, onChange: () -> Unit) =
        shared.subscribe(topics.map(tenant::invoke).toSet(), onChange)
}

private class TenantPresence(private val shared: Presence, private val tenant: TenantId) : Presence {
    override fun present(topics: Map<Topic, KClass<out Any>>): Map<Topic, Map<PresenceMember, Any?>> {
        val present = shared.present(topics.mapKeys { (topic, _) -> tenant(topic) })
        return topics.mapValues { (topic, _) -> present.getValue(tenant(topic)) }
    }

    override fun set(topic: Topic, member: PresenceMember, value: Any) = shared.set(tenant(topic), member, value)

    override fun join(topic: Topic, member: PresenceMember) = shared.join(tenant(topic), member)
}
