package org.http4k.http.lens

import org.http4k.http.core.Status
import org.http4k.http.core.Status.Companion.BAD_REQUEST

data class LensFailure(val failures: List<Failure>, val status: Status = BAD_REQUEST) : Exception(failures.map { it.toString() }.joinToString()) {

    constructor(vararg failures: Failure, status: Status = BAD_REQUEST) : this(failures.asList(), status)

    companion object {
        operator fun invoke(vararg failures: Failure) = LensFailure(failures.toList())
    }
}

sealed class Failure {
    abstract val meta: Meta
}

data class Missing(override val meta: Meta) : Failure()

data class Invalid(override val meta: Meta) : Failure()


fun Lens<*, *>.invalid() = Invalid(this.meta)
fun Lens<*, *>.missing() = Missing(this.meta)
fun Meta.missing() = Missing(this)
