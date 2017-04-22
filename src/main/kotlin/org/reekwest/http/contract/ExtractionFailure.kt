package org.reekwest.http.contract

sealed class ExtractionFailure {
    abstract val meta: Meta
}

data class Missing(override val meta: Meta) : ExtractionFailure() {
    constructor(lens: Lens<*, *>) : this(lens.meta)
}
data class Invalid(override val meta: Meta) : ExtractionFailure() {
    constructor(lens: Lens<*, *>) : this(lens.meta)
}

data class ContractBreach(val failures: List<ExtractionFailure>) : Exception(failures.map { it.toString() }.joinToString()) {

    constructor(vararg failures: ExtractionFailure): this(failures.asList())

    companion object {
        operator fun invoke(vararg failures: ExtractionFailure) = ContractBreach(failures.toList())
    }
}