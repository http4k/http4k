package org.http4k.contract

import org.http4k.core.Filter
import org.http4k.core.Method
import org.http4k.lens.LensFailure
import org.http4k.lens.Validator

internal fun PreFlightExtractionFilter(meta: RouteMeta, preFlightExtraction: PreFlightExtraction): Filter {
    val preFlightChecks = (meta.preFlightExtraction ?: preFlightExtraction)(meta)
    return Filter { next ->
        {
            when (it.method) {
                Method.OPTIONS -> next(it)
                else -> {
                    val failures = Validator.Strict(it, preFlightChecks)
                    if (failures.isEmpty()) next(it) else throw LensFailure(failures, target = it)
                }
            }
        }
    }
}
