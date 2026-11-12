/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.testing

import dev.forkhandles.values.Value
import org.http4k.config.Environment
import org.http4k.fanbelt.bus.Subscription
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.feature.Operation
import org.http4k.fanbelt.feature.execute
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.state.PresenceState
import org.http4k.fanbelt.state.State
import org.http4k.fanbelt.state.reading
import org.http4k.fanbelt.tenancy.TenantId
import java.time.Duration
import java.time.Instant

class TestTenant(val runtime: TestRuntime = TestRuntime()) {
    val scheduler = runtime.scheduler
    val events = runtime.events
    val infra = AppInfra(runtime, env = Environment.EMPTY).apply { start() }
    val clock = runtime.clock
    val random = runtime.random
    val scope = infra.scope(TenantId.of("acme"))

    fun <Actor, Input, Result : Any> execute(operation: Operation<Actor, Input, Result>, actor: Actor, input: Input): Verdict {
        val outcome = scope.execute(operation, actor, input)
        settle()
        return outcome.asVerdict()
    }

    fun <Input, Result : Any> execute(operation: Operation<Unit, Input, Result>, input: Input) = execute(operation, Unit, input)

    private val seen = mutableMapOf<Pair<State<*, *>, Any>, Any?>()
    private val announced = mutableSetOf<Pair<State<*, *>, Any>>()
    private val watched = mutableMapOf<Pair<State<*, *>, Any>, Set<Topic>>()
    private val stale = mutableMapOf<Pair<State<*, *>, Any>, Instant?>()

    fun <Key : Any, Model> read(state: State<Key, Model>, key: Key): Model {
        val read = state to key
        val snapshot = scope.reading { ctx -> ctx.read(state, key) }
        val now = snapshot.value
        val first = read !in seen
        val before = seen.put(read, now)
        val wentStale = stale.put(read, snapshot.staleAt)?.let { clock.instant() >= it } ?: false
        val unwatched = snapshot.topics - (watched[read] ?: emptySet())
        if (unwatched.isNotEmpty()) scope.bus.subscribe(unwatched) { announced += read }
        watched[read] = (watched[read] ?: emptySet()) + unwatched
        if (!first) check(before == now || read in announced || wentStale) { "${state.name}($key) changed without being published:\n  before $before\n  after  $now" }
        announced -= read
        return now
    }

    fun <Model> read(state: State<Unit, Model>) = read(state, Unit)

    fun <Key : Any, Member : Value<Primitive>, Primitive : Any> join(state: PresenceState<Key, Member, Primitive, *>, key: Key, who: Member): Subscription {
        val joined = state.join(scope, key, who)
        settle()
        return Subscription {
            joined.close()
            settle()
        }
    }

    fun <Key : Any, Member : Value<Primitive>, Primitive : Any, Carried : Any> set(state: PresenceState<Key, Member, Primitive, Carried>, key: Key, who: Member, value: Carried) {
        state.set(scope, key, who, value)
        settle()
    }

    fun <Member : Value<Primitive>, Primitive : Any, Carried : Any> set(state: PresenceState<Unit, Member, Primitive, Carried>, who: Member, value: Carried) = set(state, Unit, who, value)

    fun settle() = scheduler.runUntilIdle()

    fun tick(by: Duration) {
        scheduler.tick(by)
        settle()
    }
}
