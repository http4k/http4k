package org.http4k.fanbelt.input

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.fanbelt.feature.Outcome.Rejected

/**
 * How an operation route answers the browser.
 */
interface Feedback<in Result : Any> {
    fun rejected(request: Request, rejected: Rejected): Response
    fun accepted(request: Request, result: Result, reset: Map<String, Any?>): Response
}
