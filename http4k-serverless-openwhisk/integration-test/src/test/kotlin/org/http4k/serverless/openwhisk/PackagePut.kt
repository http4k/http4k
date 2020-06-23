package org.http4k.serverless.openwhisk

data class PackagePut(
    val namespace: String?,
    val name: String?,
    val version: String?,
    val publish: Boolean?,
    val annotations: List<KeyValue>?,
    val parameters: List<KeyValue>?,
    val binding: PackageBinding?
)
