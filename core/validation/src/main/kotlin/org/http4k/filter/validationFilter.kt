package org.http4k.filter

import dev.forkhandles.values.Problem
import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.UNSUPPORTED_MEDIA_TYPE
import org.http4k.lens.LensFailure
import org.http4k.lens.problems

fun ServerFilters.CatchValidationFailure(
    statusFor: (Problem) -> Status = { problem ->
        if (problem.constraint == "Unsupported") UNSUPPORTED_MEDIA_TYPE else BAD_REQUEST
    }
) = Filter { next ->
    { request ->
        try {
            next(request)
        } catch (e: LensFailure) {
            if (e.target is Response) throw e
            val problems = e.problems()
            Response(statusFor(problems.first())).body(problems.joinToString("; ") { it.message })
        }
    }
}
