package org.http4k.fanbelt.input

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.datastar.Selector
import org.http4k.lens.datastarGoTo

/**
 * Feedback that sends the browser to a location built from the result on acceptance.
 */
class GoTo<Result : Any>(
    private val feedback: Feedback<Result> = ErrorSignals(),
    private val where: (Result) -> Uri,
) : Feedback<Result> by feedback {
    override fun accepted(request: Request, result: Result, reset: Map<String, Any?>) =
        Response(OK).datastarGoTo(where(result), Selector.of("body"))
}
