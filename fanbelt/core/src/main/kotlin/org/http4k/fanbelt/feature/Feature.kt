package org.http4k.fanbelt.feature

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import org.http4k.fanbelt.job.Job
import org.http4k.fanbelt.job.JobName
import org.http4k.fanbelt.state.DerivedState
import org.http4k.fanbelt.state.PresenceState
import org.http4k.fanbelt.state.State
import org.http4k.fanbelt.state.StateContext
import org.http4k.fanbelt.state.StateName
import org.http4k.fanbelt.state.StoredState
import org.http4k.fanbelt.state.Tracker
import org.http4k.fanbelt.store.LockKey
import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.tenancy.Scope
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

/**
 * One area of an app: its states, rules and operations, declared as properties. Domain is built from each transaction's
 * tables; Actor is who acts.
 */
abstract class Feature<Domain, Actor : Any>(internal val domain: (Tables) -> Domain) {
    /**
     * A stored state with no key. Operations publish its topic in Changes when they change it.
     */
    protected fun <Model> state(read: (Domain) -> Model) =
        declared { name -> StoredState(StateName.of(name), domain, { _: Unit -> "" }, { domain, _ -> read(domain) }) }

    /**
     * A stored state per key. Operations publish the key's topic in Changes when they change it.
     */
    protected fun <Key : Value<*>, Model> state(read: (Domain, Key) -> Model) =
        declared { name -> StoredState(StateName.of(name), domain, { key: Key -> key.value.toString() }, read) }

    /**
     * A state computed from other states, watched topics and the domain. It updates when any of them is published.
     */
    protected fun <Key : Any, Model> derived(read: (StateContext<Domain>, Key) -> Model) =
        declared<State<Key, Model>> { name -> DerivedState(StateName.of(name), domain, read) }

    /**
     * Who's present, with no key. Joining and leaving publish its topic.
     */
    protected fun <Member : Value<Primitive>, Primitive : Any> presence(members: ValueFactory<Member, Primitive>) =
        declared { name -> PresenceState<Unit, Member, Primitive, Nothing>(StateName.of(name), { "" }, members, null) }

    /**
     * Who's present, per key. Joining and leaving publish its topic.
     */
    protected fun <Key : Value<KeyPrimitive>, KeyPrimitive : Any, Member : Value<Primitive>, Primitive : Any> presence(
        keys: ValueFactory<Key, KeyPrimitive>,
        members: ValueFactory<Member, Primitive>
    ) = declared { name -> PresenceState<Key, Member, Primitive, Nothing>(StateName.of(name), keys::show, members, null) }

    /**
     * Who's present, with no key, each member carrying a value such as a cursor until their last lease ends. Name the
     * argument: `presence(ViewerId, carrying = CellRef)`, as without it this is a presence keyed by `ViewerId`.
     */
    @JvmName("presenceCarrying")
    protected fun <Member : Value<Primitive>, Primitive : Any, Carried : Value<*>> presence(
        members: ValueFactory<Member, Primitive>,
        carrying: ValueFactory<Carried, *>
    ) = declared { name -> PresenceState(StateName.of(name), { _: Unit -> "" }, members, carrying) }

    /**
     * Who's present, per key, each member carrying a value such as a cursor until their last lease ends.
     */
    protected fun <Key : Value<*>, Member : Value<Primitive>, Primitive : Any, Carried : Value<*>> presence(
        keys: ValueFactory<Key, *>,
        members: ValueFactory<Member, Primitive>,
        carrying: ValueFactory<Carried, *>,
    ) = declared { name -> PresenceState(StateName.of(name), keys::show, members, carrying) }

    /**
     * A rule on an actor and a key. It is a derived state, so operations, regions and routes can share it.
     */
    protected fun <Key : Any, Found> rule(check: (StateContext<Domain>, Actor, Key) -> Access<Found>) =
        derived<ActorKey<Actor, Key>, Access<Found>> { ctx, (actor, key) -> check(ctx, actor, key) }

    /**
     * A rule on the actor alone.
     */
    protected fun <Found> rule(check: (StateContext<Domain>, Actor) -> Access<Found>) = rule<Unit, Found> { ctx, actor, _ -> check(ctx, actor) }

    /**
     * A job per key, of the type keys makes: an operation that names job.topic(key) in Changes has it run with that key,
     * which the body reads what it needs by. Its recorded rows carry its name, the property's unless given.
     */
    protected fun <Key : Value<*>> job(keys: ValueFactory<Key, *>, name: JobName? = null, run: (Scope, Key) -> Unit) =
        declared { property -> Job((name ?: JobName.of(property)).value, keys, run) }

    /**
     * An operation with no rule. decide runs in one write transaction.
     */
    protected fun <Input, Result : Any> operation(decide: (Domain, Actor, Input) -> Outcome<Result>) =
        operation<Input, Unit, Result>({ null }, { _, _, _ -> }) { domain, actor, input, _ -> decide(domain, actor, input) }

    /**
     * An operation guarded by a rule on a key taken from its input. decide runs only if the rule allows, in the same
     * transaction, and gets what the rule found.
     */
    protected fun <Input, Key : Any, Found, Result : Any> operation(
        rule: Rule<Actor, Key, Found>,
        key: (Input) -> Key,
        decide: (Domain, Actor, Input, Found) -> Outcome<Result>,
    ) = operation({ input -> lockKey(key(input)) }, { tracker, actor, input -> rule.found(tracker, ActorKey(actor, key(input))) }, decide)

    /**
     * An operation guarded by a rule keyed on the whole input.
     */
    protected fun <Input : Any, Found, Result : Any> operation(
        rule: Rule<Actor, Input, Found>,
        decide: (Domain, Actor, Input, Found) -> Outcome<Result>,
    ) = operation(rule, { it }, decide)

    /**
     * An operation guarded by a rule on the actor alone.
     */
    @JvmName("operationForUnitRule")
    protected fun <Input, Found, Result : Any> operation(
        rule: Rule<Actor, Unit, Found>,
        decide: (Domain, Actor, Input, Found) -> Outcome<Result>,
    ) = operation(rule, { _: Input -> }, decide)

    private fun <Input, Found, Result : Any> operation(
        key: (Input) -> LockKey?,
        found: (Tracker, Actor, Input) -> Found,
        decide: (Domain, Actor, Input, Found) -> Outcome<Result>,
    ) = declared { name ->
        Operation<Actor, Input, Result>(name, key) { tracker, actor, input ->
            decide(domain(tracker.tables), actor, input, found(tracker, actor, input))
        }
    }
}

private fun <S> declared(build: (String) -> S) =
    PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, S>> { _, property ->
        val declared = build(property.name)
        ReadOnlyProperty { _, _ -> declared }
    }

private fun lockKey(key: Any) = if (key == Unit) null else LockKey.of(key.toString())
