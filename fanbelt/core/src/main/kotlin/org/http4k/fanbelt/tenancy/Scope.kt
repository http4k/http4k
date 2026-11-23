/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.tenancy

import org.http4k.events.Events
import org.http4k.fanbelt.bus.Bus
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.events.Executed
import org.http4k.fanbelt.events.Published
import org.http4k.fanbelt.events.eventTenant
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Operation
import org.http4k.fanbelt.feature.OperationRefused
import org.http4k.fanbelt.feature.Outcome
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Hidden
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.feature.Refusal
import org.http4k.fanbelt.feature.Refused
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.job.JobTables
import org.http4k.fanbelt.presence.Presence
import org.http4k.fanbelt.presence.PresenceMember
import org.http4k.fanbelt.state.State
import org.http4k.fanbelt.state.Tracker
import org.http4k.fanbelt.state.reading
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.Tables
import java.time.Duration
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit.NANOSECONDS

/**
 * One tenant: executes operations and reads states and domains. Cheap to build per call.
 */
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

    /**
     * Runs an operation as the actor in one write transaction. A refusal rolls back; Changes are published after commit.
     */
    fun <Actor, Input, Result : Any> execute(operation: Operation<Actor, Input, Result>, actor: Actor, input: Input): Outcome<Result> {
        val outcome = try {
            storage.write { tenants ->
                val tables = tenants.tables(tenant, operation.key(input))
                when (val decided = operation.decide(Tracker(tenant, tables, null, runtime.clock.instant()), actor, input)) {
                    is Changes -> decided.also { JobTables(tables).record(it.topics) }
                    is Rejected -> throw Refused(decided)
                    Denied -> throw Refused(Denied)
                    Hidden -> throw Refused(Hidden)
                }
            }
        } catch (refused: Refused) {
            val rule = refused.rule
            if (rule != null && refused.outcome is Refusal) events(OperationRefused(operation.name, rule, refused.outcome))
            refused.outcome
        }
        events(Executed(operation.name, actor, outcome))
        if (outcome is Changes) bus.publish(outcome.topics)
        return outcome
    }

    /**
     * Reads one key of a state in a read transaction, for background work and webhooks. Its topics are dropped, so it isn't watched.
     */
    fun <Key : Any, Model> read(state: State<Key, Model>, key: Key): Model = reading { ctx -> ctx.read(state, key) }.value

    /**
     * Reads a state with no key in a read transaction, for background work and webhooks. Its topics are dropped, so it isn't watched.
     */
    fun <Model> read(state: State<Unit, Model>): Model = read(state, Unit)

    internal val presence: Presence = TenantPresence(presence, tenant)

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
    override fun present(topics: Set<Topic>): Map<Topic, Map<PresenceMember, String?>> {
        val present = shared.present(topics.map(tenant::invoke).toSet())
        return topics.associateWith { topic -> present.getValue(tenant(topic)) }
    }

    override fun set(topic: Topic, member: PresenceMember, value: String) = shared.set(tenant(topic), member, value)

    override fun join(topic: Topic, member: PresenceMember) = shared.join(tenant(topic), member)
}
