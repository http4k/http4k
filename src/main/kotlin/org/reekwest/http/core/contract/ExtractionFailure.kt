package org.reekwest.http.core.contract

sealed class ExtractionFailure {
    abstract val meta: Meta
}

data class Missing(override val meta: Meta) : ExtractionFailure()
data class Invalid(override val meta: Meta) : ExtractionFailure()
