package org.http4k.serverless.openwhisk

import java.math.BigInteger

data class Rule(
    val namespace: String,
    val name: String,
    val version: String,
    val publish: Boolean,
    val annotations: List<KeyValue>?,
    val status: String?,
    val updated: BigInteger?,
    val trigger: PathName,
    val action: PathName
)
