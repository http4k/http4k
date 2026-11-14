/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.feature

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import org.http4k.fanbelt.events.Executed
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Hidden
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.state.DerivedState
import org.http4k.fanbelt.state.PresenceState
import org.http4k.fanbelt.state.State
import org.http4k.fanbelt.state.StateContext
import org.http4k.fanbelt.state.StateName
import org.http4k.fanbelt.state.StoredState
import org.http4k.fanbelt.state.Tracker
import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.tenancy.Scope
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * One area of an app: its states, rules and operations, declared as properties. Domain is built from each transaction's
 * tables; Actor is who acts.
 */
abstract class Feature<Domain, Actor : Any>(internal val domain: (Tables) -> Domain) {
    /**
     * A stored state with no key. Operations publish its topic in Changes when they change it.
     */
    protected fun <Model> state(read: (Domain) -> Model) =
        Declared { name -> StoredState(StateName.of(name), domain, { _: Unit -> "" }) { domain, _ -> read(domain) } }

    /**
     * A stored state per key. Operations publish the key's topic in Changes when they change it.
     */
    protected fun <Key : Value<*>, Model> state(read: (Domain, Key) -> Model) =
        Declared { name -> StoredState(StateName.of(name), domain, { key: Key -> key.toString() }, read) }

    /**
     * A state computed from other states, watched topics and the domain. It updates when any of them is published.
     */
    protected fun <Key : Any, Model> derived(read: (StateContext<Domain>, Key) -> Model) =
        Declared<State<Key, Model>> { name -> DerivedState(StateName.of(name), domain, read) }

    /**
     * Who's present, with no key. Joining and leaving publish its topic.
     */
    protected fun <Member : Value<Primitive>, Primitive : Any> presence(members: ValueFactory<Member, Primitive>) =
        Declared { name -> PresenceState<Unit, Member, Primitive, Nothing>(StateName.of(name), { "" }, members, null) }

    /**
     * Who's present, per key. Joining and leaving publish its topic.
     */
    protected fun <Key : Value<KeyPrimitive>, KeyPrimitive : Any, Member : Value<Primitive>, Primitive : Any> presence(
        keys: ValueFactory<Key, KeyPrimitive>,
        members: ValueFactory<Member, Primitive>
    ) = Declared { name -> PresenceState<Key, Member, Primitive, Nothing>(StateName.of(name), keys::show, members, null) }

    /**
     * Who's present, with no key, each member carrying a value such as a cursor until their last lease ends. Name the
     * argument: `presence(ViewerId, carrying = CellRef)`, as without it this is a presence keyed by `ViewerId`.
     */
    @JvmName("presenceCarrying")
    protected fun <Member : Value<Primitive>, Primitive : Any, Carried : Value<*>> presence(
        members: ValueFactory<Member, Primitive>,
        carrying: ValueFactory<Carried, *>
    ) = Declared { name -> PresenceState(StateName.of(name), { _: Unit -> "" }, members, carrying) }

    /**
     * Who's present, per key, each member carrying a value such as a cursor until their last lease ends.
     */
    protected fun <Key : Value<*>, Member : Value<Primitive>, Primitive : Any, Carried : Value<*>> presence(
        keys: ValueFactory<Key, *>,
        members: ValueFactory<Member, Primitive>,
        carrying: ValueFactory<Carried, *>,
    ) = Declared { name -> PresenceState(StateName.of(name), keys::show, members, carrying) }

    /**
     * A rule on an actor and a key. It is a derived state, so operations, regions and routes can share it.
     */
    protected fun <Key : Any, Found> rule(check: (StateContext<Domain>, Actor, Key) -> Access<Found>) =
        derived<Attempt<Actor, Key>, Access<Found>> { ctx, (actor, key) -> check(ctx, actor, key) }

    /**
     * A rule on the actor alone.
     */
    protected fun <Found> rule(check: (StateContext<Domain>, Actor) -> Access<Found>) = rule<Unit, Found> { ctx, actor, _ -> check(ctx, actor) }

    /**
     * An operation with no rule. decide runs in one write transaction.
     */
    protected fun <Input, Result : Any> operation(decide: (Domain, Actor, Input) -> Outcome<Result>) =
        operation<Input, Unit, Result>({ _, _, _ -> }) { domain, actor, input, _ -> decide(domain, actor, input) }

    /**
     * An operation guarded by a rule on a key taken from its input. decide runs only if the rule allows, in the same
     * transaction, and gets what the rule found.
     */
    protected fun <Input, Key : Any, Found, Result : Any> operation(
        rule: Rule<Actor, Key, Found>,
        key: (Input) -> Key,
        decide: (Domain, Actor, Input, Found) -> Outcome<Result>,
    ) = operation({ tracker, actor, input -> rule.found(tracker, Attempt(actor, key(input))) }, decide)

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
        found: (Tracker, Actor, Input) -> Found,
        decide: (Domain, Actor, Input, Found) -> Outcome<Result>,
    ) = Declared { name ->
        Operation<Actor, Input, Result>(name) { tracker, actor, input ->
            decide(domain(tracker.tables), actor, input, found(tracker, actor, input))
        }
    }

    /**
     * A declaration, delegated to with by and named after its property.
     */
    protected class Declared<S>(private val build: (String) -> S) {
        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, S> {
            val declared = build(property.name)
            return ReadOnlyProperty { _, _ -> declared }
        }
    }
}

private fun <Actor : Any, Key : Any, Found> Rule<Actor, Key, Found>.found(tracker: Tracker, attempt: Attempt<Actor, Key>): Found =
    when (val access = read(tracker, attempt)) {
        is Allowed -> access.found
        is Rejected -> throw Refused(access)
        Denied -> throw Refused(Denied, name.value)
        Hidden -> throw Refused(Hidden, name.value)
    }

/**
 * Runs an operation as the actor in one write transaction. A refusal rolls back; Changes are published after commit.
 */
fun <Actor, Input, Result : Any> Scope.execute(operation: Operation<Actor, Input, Result>, actor: Actor, input: Input): Outcome<Result> {
    val outcome = decide(operation, actor, input)
    events(Executed(operation.name, actor.toString(), outcome))
    if (outcome is Changes) bus.publish(outcome.topics)
    return outcome
}

private fun <Actor, Input, Result : Any> Scope.decide(operation: Operation<Actor, Input, Result>, actor: Actor, input: Input): Outcome<Result> =
    try {
        changing {
            when (val outcome = operation.decide(Tracker(it, null), actor, input)) {
                is Changes -> outcome
                is Rejected -> throw Refused(outcome)
                Denied -> throw Refused(Denied)
                Hidden -> throw Refused(Hidden)
            }
        }
    } catch (refused: Refused) {
        val rule = refused.rule
        if (rule != null && refused.outcome is Refusal) events(OperationRefused(operation.name, rule, refused.outcome))
        refused.outcome
    }

private class Refused(val outcome: Outcome<Nothing>, val rule: String? = null) :
    RuntimeException(null, null, false, false)
