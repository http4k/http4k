/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.input

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.datastar.MorphMode.append
import org.http4k.datastar.Selector
import org.http4k.lens.datastarElements

class GoTo<Result : Any>(
    private val feedback: Feedback<Result> = ErrorSignals(),
    private val target: Selector = Selector.of("#goto"),
    private val where: (Result) -> Uri,
) : Feedback<Result> by feedback {
    override fun accepted(request: Request, result: Result, reset: Map<String, Any?>) =
        Response(OK).datastarElements(
            goTo(where(result)),
            morphMode = append,
            selector = target
        )
}

fun goTo(location: Uri, id: String? = null) =
    """<script${id?.let { " id=\"$it\"" }.orEmpty()}>window.location.assign(${scriptString(location.toString())})</script>"""

private fun scriptString(value: String) = SignalsJson.asFormatString(value).replace("</", "<\\/")
