package org.http4k.serverless.testing.client.model

data class Rule(
    val namespace: String,
    val name: String,
    val version: String,
    val publish: Boolean,
    val annotations: List<KeyValue>?,
    val status: String?,
    val updated: Int?,
    val trigger: PathName,
    val action: PathName
)
