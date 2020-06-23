package org.http4k.serverless.testing.client.model

data class UserLimits(
    val invocationsPerMinute: Int?,
    val concurrentInvocations: Int?,
    val firesPerMinute: Int?,
    val allowedKinds: List<String>?,
    val storeActivations: Boolean?
)
