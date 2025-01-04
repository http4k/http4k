package org.http4k.contract.openapi

enum class OpenApiVersion {
    _2_0_0, _3_0_0, _3_1_0;

    override fun toString() = name.removePrefix("_").replace('_', '.')
}
