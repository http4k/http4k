package org.http4k.lens

import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_REQUEST

data class LensFailure(val failures: List<Failure>, val status: Status = BAD_REQUEST, override val cause: Exception? = null) : Exception(failures.map { it.toString() }.joinToString(), cause) {

    constructor(vararg failures: Failure, status: Status = BAD_REQUEST, cause: Exception? = null) : this(failures.asList(), status, cause)

    companion object {
        operator fun invoke(vararg failures: Failure, status: Status = BAD_REQUEST, cause: Exception? = null) = LensFailure(failures.toList(), status, cause)
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
fun Meta.invalid() = Invalid(this)
