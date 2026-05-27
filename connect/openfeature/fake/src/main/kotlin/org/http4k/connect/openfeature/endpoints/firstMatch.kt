package org.http4k.connect.openfeature.endpoints

import org.http4k.connect.openfeature.FlagRule
import org.http4k.connect.openfeature.model.EvaluationContext
import org.http4k.connect.openfeature.model.FlagKey
import org.http4k.connect.openfeature.model.Reason.STATIC

internal fun List<FlagRule>.firstMatch(key: FlagKey, context: EvaluationContext): FlagRule? {
    val candidates = filter { it.key == key && it.matches(context) }
    return candidates.firstOrNull { it.reason != STATIC } ?: candidates.firstOrNull()
}
