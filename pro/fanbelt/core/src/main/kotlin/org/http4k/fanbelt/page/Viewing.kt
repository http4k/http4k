/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.fanbelt.feature.Access
import org.http4k.fanbelt.feature.ActorKey
import org.http4k.fanbelt.feature.Rule
import org.http4k.fanbelt.state.StateContext
import org.http4k.fanbelt.tenancy.Scope

/**
 * Who is viewing a page and what they may see: actor names the viewer in traces, access is the rule they must pass to
 * see the page at all, onRefused is how a viewer it refuses leaves, and onConnect runs when the stream opens, its
 * result closed when it ends.
 */
class Viewing private constructor(
    internal val actor: (Request) -> Any,
    internal val access: ((StateContext<Unit>, Request) -> Access<*>)?,
    internal val onConnect: (Scope, Request) -> AutoCloseable,
    internal val onRefused: Exit,
) {
    companion object {
        /**
         * A page anyone in the tenant may see.
         */
        operator fun invoke(
            actor: (Request) -> Any = { },
            onConnect: (Scope, Request) -> AutoCloseable = { _, _ -> AutoCloseable {} },
        ) = Viewing(actor, null, onConnect, Exit.Reload)

        /**
         * A page only viewers the rule allows may see. A refused viewer leaves by onRefused, on arrival or when the rule
         * stops allowing them while the page is open; a refused stream gets 403 for Denied and 404 for Hidden.
         */
        operator fun <Actor : Any, Key : Any, Found> invoke(
            viewer: (Request) -> ActorKey<Actor, Key>,
            access: Rule<Actor, Key, Found>,
            onRefused: Exit = Exit.GoTo(Uri.of("/")),
            onConnect: (Scope, Request) -> AutoCloseable = { _, _ -> AutoCloseable {} },
        ) = Viewing({ viewer(it).actor }, { ctx, request -> ctx.read(access, viewer(request)) }, onConnect, onRefused)
    }
}
