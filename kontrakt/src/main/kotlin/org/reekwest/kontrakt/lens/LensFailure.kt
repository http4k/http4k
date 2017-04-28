package org.reekwest.kontrakt.lens

import org.reekwest.http.core.Filter
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status
import org.reekwest.http.core.Status.Companion.BAD_REQUEST

data class LensFailure(val failures: List<Failure>, val status: Status = BAD_REQUEST) : Exception(failures.map { it.toString() }.joinToString()) {

    constructor(vararg failures: Failure, status: Status = BAD_REQUEST) : this(failures.asList(), status)

    companion object {
        operator fun invoke(vararg failures: Failure) = LensFailure(failures.toList())
    }
}

sealed class Failure {
    abstract val meta: Meta
}

data class Missing(override val meta: Meta) : Failure() {
    constructor(lens: Lens<*, *>) : this(lens.meta)
}

data class Invalid(override val meta: Meta) : Failure() {
    constructor(lens: Lens<*, *>) : this(lens.meta)
}

object CatchContractBreach : Filter {
    override fun invoke(next: HttpHandler): HttpHandler = {
        try {
            next(it)
        } catch (lensFailure: LensFailure) {
            Response(lensFailure.status)
        }
    }
}