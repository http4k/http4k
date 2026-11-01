package org.http4k.filter

import dev.forkhandles.values.Problem
import dev.forkhandles.values.causes
import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.lens.LensFailure

fun ServerFilters.CatchValidationFailure() = Filter { next ->
    { request ->
        try {
            next(request)
        } catch (e: LensFailure) {
            val problem = e.target as? Problem
            if (problem == null) throw e
            Response(BAD_REQUEST).body(problem.causes().joinToString("; ") { it.message })
        }
    }
}
