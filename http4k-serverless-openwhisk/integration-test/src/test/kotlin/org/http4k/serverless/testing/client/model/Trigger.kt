package org.http4k.serverless.testing.client.model

data class Trigger(
    val namespace: String,
    val name: String,
    val version: String,
    val publish: Boolean,
    val annotations: List<KeyValue>?,
    val parameters: List<KeyValue>?,
    val limits: TriggerLimits?,
    val rules: Map<String, Any>?,
    val updated: Int?
)
