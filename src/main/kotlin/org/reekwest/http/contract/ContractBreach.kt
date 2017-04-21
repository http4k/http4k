package org.reekwest.http.contract

data class ContractBreach(val failures: List<ExtractionFailure>) : Exception(failures.map { it.toString() }.joinToString()) {
    companion object {
        operator fun invoke(vararg failures: ExtractionFailure) = ContractBreach(failures.toList())
        fun Missing(vararg lenses: MetaLens<*, *, *>) = ContractBreach(lenses.map { Missing(it.meta) }.toList())
        fun Invalid(vararg lenses: MetaLens<*, *, *>) = ContractBreach(lenses.map { Invalid(it.meta) }.toList())
    }
}