package org.http4k.serverless.openwhisk

data class PackagePut(
    val namespace: String?,
    val name: String?,
    val version: String? = null,
    val publish: Boolean? = null,
    val annotations: List<KeyValue>? = null,
    val parameters: List<KeyValue>? = null,
    val binding: PackageBinding? = null
)
