package org.http4k.serverless.openwhisk

data class RulePut(
    val name: String?,
    val version: String?,
    val publish: Boolean?,
    val annotations: List<KeyValue>?,
    val status: String?,
    val trigger: String?,
    val action: String?
)
