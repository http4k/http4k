package org.http4k.fanbelt.testing

import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Outcome

/**
 * The result of an accepted operation, failing otherwise.
 */
fun <Result> Outcome<Result>.accepted(): Result = when (this) {
    is Changes -> result
    else -> error("expected the operation to be accepted, but it was $this")
}
