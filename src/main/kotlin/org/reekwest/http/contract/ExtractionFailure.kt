package org.reekwest.http.contract

sealed class ExtractionFailure {
    abstract val meta: Meta
}

data class Missing(override val meta: Meta) : ExtractionFailure()
data class Invalid(override val meta: Meta) : ExtractionFailure()
