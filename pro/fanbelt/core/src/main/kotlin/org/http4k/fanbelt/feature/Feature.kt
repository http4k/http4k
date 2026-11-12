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

abstract class Feature<Domain, Actor : Any>(internal val domain: (Tables) -> Domain) {
    protected fun <Model> state(read: (Domain) -> Model) =
        Declared { name -> StoredState(StateName.of(name), domain, { _: Unit -> "" }) { domain, _ -> read(domain) } }

    protected fun <Key : Value<*>, Model> state(keys: ValueFactory<Key, *>, read: (Domain, Key) -> Model) =
        Declared { name -> StoredState(StateName.of(name), domain, keys::show, read) }

    protected fun <Key : Any, Model> derived(read: (StateContext<Domain>, Key) -> Model) =
        Declared<State<Key, Model>> { name -> DerivedState(StateName.of(name), domain, read) }

    protected fun <Member : Value<Primitive>, Primitive : Any> presence(members: ValueFactory<Member, Primitive>) = presence(members, Unit::class)

    protected fun <Key : Value<*>, Member : Value<Primitive>, Primitive : Any> presence(keys: ValueFactory<Key, *>, members: ValueFactory<Member, Primitive>) =
        presence(keys, members, Unit::class)

    protected fun <Member : Value<Primitive>, Primitive : Any, Carried : Any> presence(members: ValueFactory<Member, Primitive>, carrying: KClass<Carried>) =
        Declared { name -> PresenceState(StateName.of(name), { _: Unit -> "" }, members, carrying) }

    protected fun <Key : Value<*>, Member : Value<Primitive>, Primitive : Any, Carried : Any> presence(
        keys: ValueFactory<Key, *>,
        members: ValueFactory<Member, Primitive>,
        carrying: KClass<Carried>,
    ) = Declared { name -> PresenceState(StateName.of(name), keys::show, members, carrying) }

    protected fun <Key : Any, Found> rule(check: (StateContext<Domain>, Actor, Key) -> Access<Found>) =
        derived<Attempt<Actor, Key>, Access<Found>> { ctx, (actor, key) -> check(ctx, actor, key) }

    protected fun <Found> rule(check: (StateContext<Domain>, Actor) -> Access<Found>) = rule<Unit, Found> { ctx, actor, _ -> check(ctx, actor) }

    protected fun <Input, Result : Any> operation(decide: (Domain, Actor, Input) -> Outcome<Result>) =
        operation<Input, Unit, Result>({ _, _, _ -> }) { domain, actor, input, _ -> decide(domain, actor, input) }

    protected fun <Input, Key : Any, Found, Result : Any> operation(
        rule: Rule<Actor, Key, Found>,
        key: (Input) -> Key,
        decide: (Domain, Actor, Input, Found) -> Outcome<Result>,
    ) = operation({ tracker, actor, input -> rule.found(tracker, Attempt(actor, key(input))) }, decide)

    protected fun <Input : Any, Found, Result : Any> operation(
        rule: Rule<Actor, Input, Found>,
        decide: (Domain, Actor, Input, Found) -> Outcome<Result>,
    ) = operation(rule, { it }, decide)

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

    protected class Declared<S>(private val build: (String) -> S) {
        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, S> {
            val declared = build(property.name)
            return ReadOnlyProperty { _, _ -> declared }
        }
    }
}

private fun <Actor : Any, Key : Any, Found> Rule<Actor, Key, Found>.found(tracker: Tracker, attempt: Attempt<Actor, Key>): Found =
    when (val access = read(tracker, attempt)) {
        is Access.Allowed -> access.found
        Access.Hidden -> throw Refused(name.value, Refusal.Hidden)
        Access.Denied -> throw Refused(name.value, Refusal.Denied)
        is Access.Unavailable -> throw Refused(Rejected(access.field, access.message))
    }

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
        val byRule = refused.byRule
        if (byRule != null) events(OperationRefused(operation.name, byRule.rule, byRule.refusal))
        refused.outcome
    }

private class Refused(val outcome: Outcome<Nothing>, val byRule: RuleRefusal? = null) :
    RuntimeException(null, null, false, false) {
    constructor(rule: String, refusal: Refusal) : this(
        when (refusal) {
            Refusal.Hidden -> Hidden
            Refusal.Denied -> Denied
        },
        RuleRefusal(rule, refusal)
    )
}

private class RuleRefusal(val rule: String, val refusal: Refusal)
