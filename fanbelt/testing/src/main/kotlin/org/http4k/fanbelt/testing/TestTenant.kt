package org.http4k.fanbelt.testing

import dev.forkhandles.values.Value
import org.http4k.config.Environment.Companion.EMPTY
import org.http4k.fanbelt.bus.Subscription
import org.http4k.fanbelt.feature.Access
import org.http4k.fanbelt.feature.ActorKey
import org.http4k.fanbelt.feature.Operation
import org.http4k.fanbelt.feature.Rule
import org.http4k.fanbelt.feature.Visit
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.job.Job
import org.http4k.fanbelt.job.Jobs
import org.http4k.fanbelt.state.PresenceState
import org.http4k.fanbelt.state.State
import org.http4k.fanbelt.state.StateContext
import org.http4k.fanbelt.state.reading
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.infrastructure.TestRuntime
import java.time.Duration
import java.time.Instant

/**
 * Drives features for one tenant in a test, without HTTP.
 */
class TestTenant(val runtime: TestRuntime = TestRuntime(), storage: DocumentStorage = InMemoryDocumentStorage()) {
    private val infra = AppInfra(runtime, storage, env = EMPTY).apply { start() }
    val storage = infra.storage
    val clock = runtime.clock
    val random = runtime.random
    private val scope = infra.scope(TenantId.of("acme"))
    val id = scope.tenant

    private val seen = mutableMapOf<Pair<State<*, *>, Any>, Any?>()
    private val announced = mutableSetOf<Pair<State<*, *>, Any>>()
    private val listening = mutableMapOf<Pair<State<*, *>, Any>, Subscription>()
    private val stale = mutableMapOf<Pair<State<*, *>, Any>, Instant?>()
    private val rereads = mutableMapOf<Pair<State<*, *>, Any>, (StateContext<Unit>) -> Any?>()

    fun runs(vararg jobs: Job<*>) = Jobs(infra, *jobs).start()

    @PublishedApi
    internal val steps = Steps()

    inline operator fun <R> invoke(block: Steps.() -> R): R = steps.block()

    /**
     * What a test can do inside the tenant.
     */
    inner class Steps internal constructor() {
        internal val scope get() = this@TestTenant.scope

        /**
         * Runs an operation as the actor.
         */
        fun <Actor, Input, Result : Any> execute(operation: Operation<Actor, Input, Result>, actor: Actor, input: Input): Verdict =
            run(operation, actor, input).asVerdict()

        /**
         * Runs an operation with no actor.
         */
        fun <Input, Result : Any> execute(operation: Operation<Unit, Input, Result>, input: Input) = execute(operation, Unit, input)

        /**
         * Runs an operation as the actor, as setup.
         */
        fun <Actor, Input, Result : Any> given(operation: Operation<Actor, Input, Result>, actor: Actor, input: Input): Result =
            run(operation, actor, input).accepted()

        /**
         * Runs an operation with no actor, as setup.
         */
        fun <Input, Result : Any> given(operation: Operation<Unit, Input, Result>, input: Input) = given(operation, Unit, input)

        /**
         * Reads a state under the key.
         */
        @Suppress("UNCHECKED_CAST")
        fun <Key : Any, Model> read(state: State<Key, Model>, key: Key): Model {
            settle()
            val read = state to key
            rereads[read] = { ctx -> ctx.read(state, key) }
            return observe(read) as Model
        }

        /**
         * Reads a state with no key.
         */
        fun <Model> read(state: State<Unit, Model>) = read(state, Unit)

        /**
         * Reads a rule for an actor and a key.
         */
        fun <Actor : Any, Key : Any, Found> read(rule: Rule<Actor, Key, Found>, actor: Actor, key: Key): Access<Found> = read(rule, ActorKey(actor, key))

        /**
         * Reads a rule with no key, for an actor.
         */
        @JvmName("readForUnitRule")
        fun <Actor : Any, Found> read(rule: Rule<Actor, Unit, Found>, actor: Actor): Access<Found> = read(rule, actor, Unit)

        /**
         * Joins a presence under the key.
         */
        fun <Key : Any, Member : Value<Primitive>, Primitive : Any> join(presence: PresenceState<Key, Member, Primitive, *>, actor: Member, key: Key): Subscription =
            settling(scope.join(presence, actor, key))

        /**
         * Joins a presence with no key.
         */
        fun <Member : Value<Primitive>, Primitive : Any> join(presence: PresenceState<Unit, Member, Primitive, *>, actor: Member) = join(presence, actor, Unit)

        /**
         * Opens a visit with an input.
         */
        fun <Actor, Input> open(visit: Visit<Actor, Input>, actor: Actor, input: Input): Subscription = settling(scope.open(visit, actor, input))

        /**
         * Opens a visit with no input.
         */
        fun <Actor> open(visit: Visit<Actor, Unit>, actor: Actor) = open(visit, actor, Unit)

        /**
         * Carries a value in a presence under the key.
         */
        fun <Key : Any, Member : Value<Primitive>, Primitive : Any, Carried : Value<*>> carry(presence: PresenceState<Key, Member, Primitive, Carried>, actor: Member, key: Key, value: Carried) {
            scope.carry(presence, actor, key, value)
            settle()
        }

        /**
         * Carries a value in a presence with no key.
         */
        fun <Member : Value<Primitive>, Primitive : Any, Carried : Value<*>> carry(presence: PresenceState<Unit, Member, Primitive, Carried>, actor: Member, value: Carried) =
            carry(presence, actor, Unit, value)

        private fun settling(opened: AutoCloseable): Subscription {
            settle()
            return Subscription {
                opened.close()
                settle()
            }
        }

        fun <R> tables(block: (Tables) -> R): R = storage.read { block(it.tables(id)) }

        fun settle() {
            runtime.scheduler.runUntilIdle()
            rereads.keys.toList().forEach(::observe)
        }

        fun tick(by: Duration) {
            runtime.scheduler.tick(by)
            settle()
        }
    }

    private fun <Actor, Input, Result : Any> run(operation: Operation<Actor, Input, Result>, actor: Actor, input: Input) =
        scope.execute(operation, actor, input).also { steps.settle() }

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
