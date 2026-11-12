/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.testing

import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Outcome

sealed interface Verdict {
    data object Accepted : Verdict
    data class Rejected(val reasons: Set<String>) : Verdict {
        constructor(vararg reasons: String) : this(reasons.toSet())
    }

    data object Denied : Verdict

    data object Hidden : Verdict
}

fun Outcome<*>.asVerdict(): Verdict = when (this) {
    is Changes -> Verdict.Accepted
    is Outcome.Rejected -> Verdict.Rejected(errors.values.toSet())
    Outcome.Denied -> Verdict.Denied
    Outcome.Hidden -> Verdict.Hidden
}
