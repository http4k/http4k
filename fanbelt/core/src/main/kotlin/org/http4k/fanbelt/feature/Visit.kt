package org.http4k.fanbelt.feature

import org.http4k.fanbelt.page.Admitted
import org.http4k.fanbelt.tenancy.Scope

/**
 * What arriving at a page means for one actor.
 */
class Visit<Actor, Input> internal constructor(private val arrive: (Scope, Actor, Input) -> AutoCloseable) :
        (Scope, Admitted<Actor, Input, Any?>) -> AutoCloseable {
    internal fun open(scope: Scope, actor: Actor, input: Input) = arrive(scope, actor, input)

    override fun invoke(scope: Scope, admitted: Admitted<Actor, Input, Any?>) = open(scope, admitted.actor, admitted.key)

    /**
     * Binds the visit to a page, taking its actor and input from who the page admitted.
     */
    operator fun <PageActor, Key, Found> invoke(actor: (Admitted<PageActor, Key, Found>) -> Actor, input: (Admitted<PageActor, Key, Found>) -> Input) =
        { scope: Scope, admitted: Admitted<PageActor, Key, Found> -> open(scope, actor(admitted), input(admitted)) }
}

/**
 * Binds a visit with no input to a page, taking its actor from who the page admitted.
 */
operator fun <Actor, PageActor, Key, Found> Visit<Actor, Unit>.invoke(actor: (Admitted<PageActor, Key, Found>) -> Actor) = invoke(actor) { }
