package org.http4k.serverless.openwhisk

data class ActionPut(
    val namespace: String,
    val name: String,
    val version: String,
    val publish: Boolean,
    val exec: ActionExec,
    val annotations: List<KeyValue>,
    val parameters: List<KeyValue>? = null,
    val limits: ActionLimits? = null
)
