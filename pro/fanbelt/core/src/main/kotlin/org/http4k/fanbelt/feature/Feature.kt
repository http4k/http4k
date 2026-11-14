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
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * What people can see and do in one area of the app: its states, rules and operations, each declared as a property and
 * named after it. It is given the function that builds its domain from each transaction's tables, `Actor` is who acts,
 * and it is driven the same way from tests and from HTTP.
 */
abstract class Feature<Domain, Actor : Any>(internal val domain: (Tables) -> Domain) {
    /**
     * A stored state with no key: what `read` takes from the domain. Its topic is what an operation publishes in
     * `Changes` when it changes that value.
     */
    protected fun <Model> state(read: (Domain) -> Model) =
        Declared { name -> StoredState(StateName.of(name), domain, { _: Unit -> "" }) { domain, _ -> read(domain) } }

    /**
     * A stored state keyed by a value type, with a topic per key, so a change to one key redraws only what shows it.
     */
    protected fun <Key : Value<*>, Model> state(read: (Domain, Key) -> Model) =
        Declared { name -> StoredState(StateName.of(name), domain, { key: Key -> key.toString() }, read) }

    /**
     * A state computed from other states (`ctx.read`), topics that aren't states (`ctx.watch`) and the domain. It
     * redraws whenever anything it read is published, and publishes nothing of its own.
     */
    protected fun <Key : Any, Model> derived(read: (StateContext<Domain>, Key) -> Model) =
        Declared<State<Key, Model>> { name -> DerivedState(StateName.of(name), domain, read) }

    /**
     * Who's here, with no key: read as a map of present members. Joining and leaving publish, so operations never do.
     */
    protected fun <Member : Value<Primitive>, Primitive : Any> presence(members: ValueFactory<Member, Primitive>) = presence(members, Unit::class)

    /**
     * Who's here, per key: read as a map of present members. Joining and leaving publish, so operations never do.
     */
    protected fun <Key : Value<*>, Member : Value<Primitive>, Primitive : Any> presence(keys: ValueFactory<Key, *>, members: ValueFactory<Member, Primitive>) =
        presence(keys, members, Unit::class)

    /**
     * Who's here, with no key, each member carrying a value (a cursor, a selection) that goes when their last lease
     * ends.
     */
    protected fun <Member : Value<Primitive>, Primitive : Any, Carried : Any> presence(members: ValueFactory<Member, Primitive>, carrying: KClass<Carried>) =
        Declared { name -> PresenceState(StateName.of(name), { _: Unit -> "" }, members, carrying) }

    /**
     * Who's here, per key, each member carrying a value (a cursor, a selection) that goes when their last lease ends.
     */
    protected fun <Key : Value<*>, Member : Value<Primitive>, Primitive : Any, Carried : Any> presence(
        keys: ValueFactory<Key, *>,
        members: ValueFactory<Member, Primitive>,
        carrying: KClass<Carried>,
    ) = Declared { name -> PresenceState(StateName.of(name), keys::show, members, carrying) }

    /**
     * Who may act on a key: `Allowed` with what it found, `Hidden` (404), `Denied` (403) or `Rejected` (a field
     * error). Being a derived state, it is shared by operations, regions and routes, and redraws what it guards when
     * what it read changes.
     */
    protected fun <Key : Any, Found> rule(check: (StateContext<Domain>, Actor, Key) -> Access<Found>) =
        derived<Attempt<Actor, Key>, Access<Found>> { ctx, (actor, key) -> check(ctx, actor, key) }

    /**
     * A rule on the actor alone, with no key: typically narrows the actor to a kind, as `Allowed(actor)`.
     */
    protected fun <Found> rule(check: (StateContext<Domain>, Actor) -> Access<Found>) = rule<Unit, Found> { ctx, actor, _ -> check(ctx, actor) }

    /**
     * Something people can do, guarded by no rule: `decide` runs in one write transaction and answers an `Outcome`.
     */
    protected fun <Input, Result : Any> operation(decide: (Domain, Actor, Input) -> Outcome<Result>) =
        operation<Input, Unit, Result>({ _, _, _ -> }) { domain, actor, input, _ -> decide(domain, actor, input) }

    /**
     * An operation guarded by a rule on the key taken from its input. The rule is checked in the same transaction, and
     * `decide` runs only if it allows, given what the rule found; a refusal is reported as `OperationRefused`.
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
     * An operation guarded by a rule with no key, on the actor alone.
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
     * What a declaration returns: delegated to with `by`, it is built once, named after its property.
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
 * Runs an operation as the actor in one write transaction for this tenant and returns how it ended. A refusal
 * (`Rejected`, `Denied` or `Hidden`, from the operation or its rule) rolls back everything written before it; `Changes`
 * publish their topics only after commit.
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
