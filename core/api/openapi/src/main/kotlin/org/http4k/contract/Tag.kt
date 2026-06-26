package org.http4k.contract

data class Tag(
    val name: String,
    val description: String? = null,
    val summary: String? = null,
    val parent: String? = null,
    val kind: String? = null
)
