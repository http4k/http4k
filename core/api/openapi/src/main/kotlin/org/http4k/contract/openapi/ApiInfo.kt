package org.http4k.contract.openapi

data class ApiInfo(
    val title: String,
    val version: String,
    val description: String? = null,
    val summary: String? = null,
    val license: ApiLicense? = null
)

data class ApiLicense(val name: String, val identifier: String? = null, val url: String? = null) {
    companion object {
        val Apache2_0 = ApiLicense("Apache 2.0", "Apache-2.0")
        val MIT = ApiLicense("MIT", "MIT")
        val GPL3_0 = ApiLicense("GNU GPL v3.0", "GPL-3.0-only")
    }
}
