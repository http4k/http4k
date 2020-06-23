package org.http4k.serverless.testing.client.model

data class Action(
    val namespace: String,
    val name: String,
    val version: String,
    val publish: Boolean,
    val exec: ActionExec,
    val annotations: List<KeyValue>?,
    val parameters: List<KeyValue>?,
    val limits: ActionLimits,
    val updated: Int?
)
