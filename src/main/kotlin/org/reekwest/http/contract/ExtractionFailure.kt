package org.reekwest.http.contract

import org.reekwest.http.core.Status
import org.reekwest.http.core.Status.Companion.BAD_REQUEST

sealed class ExtractionFailure {
    abstract val meta: Meta
}

data class Missing(override val meta: Meta) : ExtractionFailure() {
    constructor(lens: Lens<*, *>) : this(lens.meta)
}
data class Invalid(override val meta: Meta) : ExtractionFailure() {
    constructor(lens: Lens<*, *>) : this(lens.meta)
}

data class ContractBreach(val failures: List<ExtractionFailure>, val status: Status = BAD_REQUEST) : Exception(failures.map { it.toString() }.joinToString()) {

    constructor(vararg failures: ExtractionFailure, status: Status = BAD_REQUEST): this(failures.asList(), status)

    companion object {
        operator fun invoke(vararg failures: ExtractionFailure) = ContractBreach(failures.toList())
    }
}