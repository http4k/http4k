package org.reekwest.http.contract

data class ContractBreach(val failures: List<ExtractionFailure>) : Exception(failures.map { it.toString() }.joinToString()) {

    constructor(vararg failures: ExtractionFailure): this(failures.asList())

    companion object {
        operator fun invoke(vararg failures: ExtractionFailure) = ContractBreach(failures.toList())
    }
}