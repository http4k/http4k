/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap

import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import org.http4k.security.Security
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.home.Index

fun WiretapUi(
    renderer: DatastarElementRenderer,
    html: TemplateRenderer,
    functions: List<WiretapFunction>,
    security: Security
) = ServerFilters.CatchLensFailure()
    .then(security.filter)
    .then(
        routes(
            "/_wiretap" bind routes(
                listOf(
                    Index(html),
                    static(Classpath("public"))
                ) + functions.map { it.http(renderer, html) },
            )
        )
    )
