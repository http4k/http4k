package org.http4k.contract.openapi

enum class OpenApiVersion {
    _2_0_0, _3_0_0, _3_1_0, _3_2_0;

    override fun toString() = name.removePrefix("_").replace('_', '.')

    /**
     * The default JSON Schema dialect declared by the document. Only exists from 3.1 onwards.
     */
    val jsonSchemaDialect: String?
        get() = when (this) {
            _3_1_0 -> "https://spec.openapis.org/oas/3.1/dialect/base"
            _3_2_0 -> "https://spec.openapis.org/oas/3.2/dialect/2025-09-17"
            else -> null
        }
}
