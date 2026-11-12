/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.input

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiObject
import org.http4k.format.asDatastarSignal
import org.http4k.lens.datastarSignals

class ErrorSignals(private val name: String = "errors") : Feedback<Any> {
    init {
        require(name.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) { "a Datastar signal name is a letter or _ then letters, digits or _: $name" }
    }

    override fun rejected(rejected: Rejected) =
        signals(mapOf(name to rejected.errors.mapKeys { it.key.value }))

    override fun accepted(request: Request, result: Any, reset: Map<String, Any?>) =
        signals(reset + (name to shown(request).associateWith { "" }))

    private fun shown(request: Request): Set<String> {
        val signals = runCatching { SignalsJson.asA<MoshiNode>(request.bodyString()) }.getOrNull()
        val errors = if (signals is MoshiObject) signals[name] else null
        return if (errors is MoshiObject) errors.attributes.keys else emptySet()
    }

    private fun signals(values: Map<String, Any?>) =
        Response(OK).datastarSignals(SignalsJson.asDatastarSignal(values))
}
