package org.http4k.serverless.testing.client.model

data class RulePut(
    val name: String?,
    val version: String?,
    val publish: Boolean?,
    val annotations: List<KeyValue>?,
    val status: String?,
    val trigger: String?,
    val action: String?
)
