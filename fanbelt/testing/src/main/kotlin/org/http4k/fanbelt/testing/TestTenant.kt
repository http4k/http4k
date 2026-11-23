package org.http4k.fanbelt.testing

import dev.forkhandles.values.Value
import org.http4k.config.Environment
import org.http4k.fanbelt.bus.Subscription
import org.http4k.fanbelt.feature.Operation
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.job.Job
import org.http4k.fanbelt.job.Jobs
import org.http4k.fanbelt.state.PresenceState
import org.http4k.fanbelt.state.State
import org.http4k.fanbelt.state.StateContext
import org.http4k.fanbelt.state.reading
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import java.time.Duration
import java.time.Instant

/**
 * Drives features for one tenant without HTTP, in the storage it is given (in memory by default), settling background work after each step, including the jobs
 * it is given. Every state it has read is checked again after each step, which fails if the state
 * changed without its topics being published.
 */
class TestTenant(val runtime: TestRuntime = TestRuntime(), storage: DocumentStorage = InMemoryDocumentStorage(), jobs: List<Job<*>> = emptyList()) {
    val scheduler = runtime.scheduler
    val events = runtime.events
    val infra = AppInfra(runtime, storage, env = Environment.EMPTY).apply { start() }
    val clock = runtime.clock
    val random = runtime.random
    val scope = infra.scope(TenantId.of("acme"))

    init {
        Jobs(infra, *jobs.toTypedArray()).start()
    }

    /**
     * Runs an operation as the actor, then settles.
     */
    fun <Actor, Input, Result : Any> execute(operation: Operation<Actor, Input, Result>, actor: Actor, input: Input): Verdict {
        val outcome = scope.execute(operation, actor, input)
        settle()
        return outcome.asVerdict()
    }

    fun <Input, Result : Any> execute(operation: Operation<Unit, Input, Result>, input: Input) = execute(operation, Unit, input)

    private val seen = mutableMapOf<Pair<State<*, *>, Any>, Any?>()
    private val announced = mutableSetOf<Pair<State<*, *>, Any>>()
    private val listening = mutableMapOf<Pair<State<*, *>, Any>, Subscription>()
    private val stale = mutableMapOf<Pair<State<*, *>, Any>, Instant?>()
    private val rereads = mutableMapOf<Pair<State<*, *>, Any>, (StateContext<Unit>) -> Any?>()

    /**
     * Reads a state, and from then on checks it after every step: a change without its topics published (or its stale
     * time passing) fails that step.
     */
    @Suppress("UNCHECKED_CAST")
    fun <Key : Any, Model> read(state: State<Key, Model>, key: Key): Model {
        val read = state to key
        rereads[read] = { ctx -> ctx.read(state, key) }
        return observe(read) as Model
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

    fun <Key : Any, Member : Value<Primitive>, Primitive : Any, Carried : Value<*>> set(state: PresenceState<Key, Member, Primitive, Carried>, key: Key, who: Member, value: Carried) {
        state.set(scope, key, who, value)
        settle()
    }

    fun <Member : Value<Primitive>, Primitive : Any, Carried : Value<*>> set(state: PresenceState<Unit, Member, Primitive, Carried>, who: Member, value: Carried) = set(state, Unit, who, value)

    /**
     * Runs pending background work, then checks every state read so far for a change nobody published.
     */
    fun settle() {
        scheduler.runUntilIdle()
        rereads.keys.toList().forEach(::observe)
    }

    /**
     * Moves the clock on, running what falls due, then settles.
     */
    fun tick(by: Duration) {
        scheduler.tick(by)
        settle()
    }

    private fun observe(read: Pair<State<*, *>, Any>): Any? {
        val snapshot = scope.reading(rereads.getValue(read))
        val now = snapshot.value
        val first = read !in seen
        val before = seen.put(read, now)
        val wentStale = stale.put(read, snapshot.staleAt)?.let { clock.instant() >= it } ?: false
        val published = announced.remove(read)
        listening.put(read, scope.bus.subscribe(snapshot.topics) { announced += read })?.close()
        if (!first) check(before == now || published || wentStale) { "${read.first.name}(${read.second}) changed without being published:\n  before $before\n  after  $now" }
        return now
    }
}
