package org.reekwest.http.contract

import org.reekwest.http.core.Filter
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status
import org.reekwest.http.core.Status.Companion.BAD_REQUEST

data class ContractBreach(val failures: List<ExtractionFailure>, val status: Status = BAD_REQUEST) : Exception(failures.map { it.toString() }.joinToString()) {

    constructor(vararg failures: ExtractionFailure, status: Status = BAD_REQUEST) : this(failures.asList(), status)

    companion object {
        operator fun invoke(vararg failures: ExtractionFailure) = ContractBreach(failures.toList())
    }
}

sealed class ExtractionFailure {
    abstract val meta: Meta
}

data class Missing(override val meta: Meta) : ExtractionFailure() {
    constructor(lens: Lens<*, *>) : this(lens.meta)
}

data class Invalid(override val meta: Meta) : ExtractionFailure() {
    constructor(lens: Lens<*, *>) : this(lens.meta)
}

object CatchContractBreach : Filter {
    override fun invoke(next: HttpHandler): HttpHandler = {
        try {
            next(it)
        } catch (e: ContractBreach) {
            Response(BAD_REQUEST)
        }
    }
}