package org.http4k.fanbelt.testing

import dev.forkhandles.values.Value
import org.http4k.config.Environment.Companion.EMPTY
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
import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.infrastructure.TestRuntime
import java.time.Duration
import java.time.Instant

/**
 * Drives features for one tenant without HTTP, on a node of its own, in the storage it is given (in memory by default),
 * settling background work after each step, including the jobs it [runs]. Every state it has read is checked again after
 * each step, which fails if the state changed without its topics being published.
 */
class TestTenant(val runtime: TestRuntime = TestRuntime(), storage: DocumentStorage = InMemoryDocumentStorage()) {
    private val infra = AppInfra(runtime, storage, env = EMPTY).apply { start() }
    val storage = infra.storage
    val clock = runtime.clock
    val random = runtime.random
    val scope = infra.scope(TenantId.of("acme"))
    val id = scope.tenant

    private val seen = mutableMapOf<Pair<State<*, *>, Any>, Any?>()
    private val announced = mutableSetOf<Pair<State<*, *>, Any>>()
    private val listening = mutableMapOf<Pair<State<*, *>, Any>, Subscription>()
    private val stale = mutableMapOf<Pair<State<*, *>, Any>, Instant?>()
    private val rereads = mutableMapOf<Pair<State<*, *>, Any>, (StateContext<Unit>) -> Any?>()

    /**
     * Runs these jobs on the tenant's node: a named job has run by the time the operation that named it returns, and its
     * retries run as the clock ticks.
     */
    fun runs(vararg jobs: Job<*>) = Jobs(infra, *jobs).start()

    @PublishedApi
    internal val steps = Steps()

    /**
     * Runs the block inside the tenant, where a feature's operations and states are called directly.
     */
    inline operator fun <R> invoke(block: Steps.() -> R): R = steps.block()

    /**
     * What a test can do inside the tenant: execute operations, read states, and join and carry in presence states.
     */
    inner class Steps internal constructor() {

        /**
         * Runs this operation as the actor, then settles, returning a [Verdict] on its outcome.
         */
        fun <Actor, Input, Result : Any> Operation<Actor, Input, Result>.execute(actor: Actor, input: Input): Verdict =
            run(this, actor, input).asVerdict()

        /**
         * Runs this operation, which needs no actor; see [execute].
         */
        fun <Input, Result : Any> Operation<Unit, Input, Result>.execute(input: Input) = execute(Unit, input)

        /**
         * Runs this operation as the actor as setup, then settles, returning its result. Fails if it is refused.
         */
        fun <Actor, Input, Result : Any> Operation<Actor, Input, Result>.given(actor: Actor, input: Input): Result =
            run(this, actor, input).accepted()

        /**
         * Runs this operation, which needs no actor, as setup; see [given].
         */
        fun <Input, Result : Any> Operation<Unit, Input, Result>.given(input: Input) = given(Unit, input)

        /**
         * Settles, then reads this state under the key, and from then on checks it after every step: a change without its
         * topics published (or its stale time passing) fails that step.
         */
        @Suppress("UNCHECKED_CAST")
        fun <Key : Any, Model> State<Key, Model>.read(key: Key): Model {
            settle()
            val state = this
            val read = state to key
            rereads[read] = { ctx -> ctx.read(state, key) }
            return observe(read) as Model
        }

        /**
         * Reads this state, which has no key; see [read].
         */
        fun <Model> State<Unit, Model>.read() = read(Unit)

        /**
         * Joins [who] to this presence state under [key], then settles. Closing the returned subscription leaves, then settles.
         */
        fun <Key : Any, Member : Value<Primitive>, Primitive : Any> PresenceState<Key, Member, Primitive, *>.join(key: Key, who: Member): Subscription {
            val joined = join(scope, key, who)
            settle()
            return Subscription {
                joined.close()
                settle()
            }
        }

        /**
         * Joins [who] to this presence state, which has no key; see [join].
         */
        fun <Member : Value<Primitive>, Primitive : Any> PresenceState<Unit, Member, Primitive, *>.join(who: Member) = join(Unit, who)

        /**
         * Has [who] carry a value in this presence state under [key], then settles.
         */
        fun <Key : Any, Member : Value<Primitive>, Primitive : Any, Carried : Value<*>> PresenceState<Key, Member, Primitive, Carried>.carry(key: Key, who: Member, value: Carried) {
            carry(scope, key, who, value)
            settle()
        }

        /**
         * Has [who] carry a value in this presence state, which has no key; see [carry].
         */
        fun <Member : Value<Primitive>, Primitive : Any, Carried : Value<*>> PresenceState<Unit, Member, Primitive, Carried>.carry(who: Member, value: Carried) =
            carry(Unit, who, value)
    }

    /**
     * Reads this tenant's tables directly, for rows no state shows.
     */
    fun <R> tables(block: (Tables) -> R): R = storage.read { block(it.tables(id)) }

    /**
     * Runs pending background work, then checks every state read so far for a change nobody published.
     */
    fun settle() {
        runtime.scheduler.runUntilIdle()
        rereads.keys.toList().forEach(::observe)
    }

    /**
     * Moves the clock on, running what falls due, then settles.
     */
    fun tick(by: Duration) {
        runtime.scheduler.tick(by)
        settle()
    }

    private fun <Actor, Input, Result : Any> run(operation: Operation<Actor, Input, Result>, actor: Actor, input: Input) =
        scope.execute(operation, actor, input).also { settle() }

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
