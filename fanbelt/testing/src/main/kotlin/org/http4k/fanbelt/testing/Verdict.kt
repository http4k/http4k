package org.http4k.fanbelt.testing

import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Outcome
import org.http4k.fanbelt.testing.Verdict.Accepted
import org.http4k.fanbelt.testing.Verdict.Denied
import org.http4k.fanbelt.testing.Verdict.Hidden
import org.http4k.fanbelt.testing.Verdict.Rejected

/**
 * How an action turned out: Accepted, Rejected, Denied (403) or Hidden (404).
 */
sealed interface Verdict {
    data object Accepted : Verdict
    data class Rejected(val reasons: Set<String>) : Verdict {
        constructor(vararg reasons: String) : this(reasons.toSet())
    }

    data object Denied : Verdict

    data object Hidden : Verdict
}

/**
 * The Verdict for an Outcome.
 */
fun Outcome<*>.asVerdict(): Verdict = when (this) {
    is Changes -> Accepted
    is Outcome.Rejected -> Rejected(errors.values.toSet())
    Outcome.Denied -> Denied
    Outcome.Hidden -> Hidden
}
