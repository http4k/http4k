package org.http4k.fanbelt.tenancy

import dev.forkhandles.values.Value
import java.time.Duration
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit.NANOSECONDS
import org.http4k.events.Events
import org.http4k.fanbelt.bus.Bus
import org.http4k.fanbelt.bus.Subscription
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.events.Executed
import org.http4k.fanbelt.events.Published
import org.http4k.fanbelt.events.eventTenant
import org.http4k.fanbelt.feature.Access
import org.http4k.fanbelt.feature.ActorKey
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Operation
import org.http4k.fanbelt.feature.OperationRefused
import org.http4k.fanbelt.feature.Outcome
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Hidden
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.feature.Refusal
import org.http4k.fanbelt.feature.Refused
import org.http4k.fanbelt.feature.Rule
import org.http4k.fanbelt.feature.Visit
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.job.JobTables
import org.http4k.fanbelt.presence.Presence
import org.http4k.fanbelt.presence.PresenceMember
import org.http4k.fanbelt.state.PresenceState
import org.http4k.fanbelt.state.State
import org.http4k.fanbelt.state.Tracker
import org.http4k.fanbelt.state.reading
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.Tables

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
            if (rule != null && refused.outcome is Refusal) events(OperationRefused(operation.name, actor, refused.key, rule, refused.outcome))
            refused.outcome
        }
        events(Executed(operation.name, actor, operation.about(input), outcome))
        if (outcome is Changes) bus.publish(outcome.topics)
        return outcome
    }

    /**
     * Reads a state under the key in a read transaction, for background work and webhooks. Its topics are dropped, so
     * it isn't watched.
     */
    fun <Key : Any, Model> read(state: State<Key, Model>, key: Key): Model = reading { ctx -> ctx.read(state, key) }.value

    /**
     * Reads a state with no key in a read transaction.
     */
    fun <Model> read(state: State<Unit, Model>): Model = read(state, Unit)

    /**
     * Reads a rule for an actor and a key in a read transaction.
     */
    fun <Actor : Any, Key : Any, Found> read(rule: Rule<Actor, Key, Found>, actor: Actor, key: Key): Access<Found> = read(rule, ActorKey(actor, key))

    /**
     * Reads a rule that takes no key, for an actor, in a read transaction.
     */
    @JvmName("readForUnitRule")
    fun <Actor : Any, Found> read(rule: Rule<Actor, Unit, Found>, actor: Actor): Access<Found> = read(rule, actor, Unit)

    /**
     * Runs an operation with no actor in one write transaction.
     */
    fun <Input, Result : Any> execute(operation: Operation<Unit, Input, Result>, input: Input): Outcome<Result> = execute(operation, Unit, input)

    /**
     * Joins the actor to a presence under the key; close the lease to leave.
     */
    fun <Key : Any, Member : Value<Primitive>, Primitive : Any> join(presence: PresenceState<Key, Member, Primitive, *>, actor: Member, key: Key): Subscription =
        presence.join(this, actor, key)

    /**
     * Joins the actor to a presence with no key; close the lease to leave.
     */
    fun <Member : Value<Primitive>, Primitive : Any> join(presence: PresenceState<Unit, Member, Primitive, *>, actor: Member): Subscription =
        join(presence, actor, Unit)

    /**
     * Has the actor carry a value in a presence under the key.
     */
    fun <Key : Any, Member : Value<Primitive>, Primitive : Any, Carried : Value<*>> carry(presence: PresenceState<Key, Member, Primitive, Carried>, actor: Member, key: Key, value: Carried) =
        presence.carry(this, actor, key, value)

    /**
     * Has the actor carry a value in a presence with no key.
     */
    fun <Member : Value<Primitive>, Primitive : Any, Carried : Value<*>> carry(presence: PresenceState<Unit, Member, Primitive, Carried>, actor: Member, value: Carried) =
        carry(presence, actor, Unit, value)

    /**
     * Opens a visit for the actor and input, as a page's connect would; close what it returns to leave.
     */
    fun <Actor, Input> open(visit: Visit<Actor, Input>, actor: Actor, input: Input): AutoCloseable = visit.open(this, actor, input)

    /**
     * Opens a visit with no input for the actor.
     */
    fun <Actor> open(visit: Visit<Actor, Unit>, actor: Actor): AutoCloseable = open(visit, actor, Unit)

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
