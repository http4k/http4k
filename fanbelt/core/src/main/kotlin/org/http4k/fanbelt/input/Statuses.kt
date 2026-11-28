package org.http4k.fanbelt.input

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.fanbelt.feature.Outcome.Rejected

/**
 * Feedback for a machine caller, such as a webhook, answering with status codes.
 */
class Statuses(private val accepted: Status = ACCEPTED) : Feedback<Any> {
    override fun accepted(request: Request, result: Any, reset: Map<String, Any?>) = Response(accepted)

    override fun rejected(request: Request, rejected: Rejected) = Response(BAD_REQUEST).body(rejected.errors.values.joinToString("\n"))
}
