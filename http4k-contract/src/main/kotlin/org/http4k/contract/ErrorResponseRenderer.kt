package org.http4k.contract

import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.lens.LensFailure

interface ErrorResponseRenderer {
    fun badRequest(lensFailure: LensFailure) = Response(BAD_REQUEST)
    fun notFound() = Response(NOT_FOUND)
}

