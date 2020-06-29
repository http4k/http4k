package org.http4k.serverless.openwhisk

import java.math.BigInteger

data class Action(
    val namespace: String,
    val name: String,
    val version: String,
    val publish: Boolean,
    val exec: ActionExec,
    val annotations: List<KeyValue>?,
    val parameters: List<KeyValue>?,
    val limits: ActionLimits,
    val updated: BigInteger?
)
