package org.http4k.serverless

import java.time.Duration

data class FnTransaction<In, Out>(
    val request: In,
    val response: Out,
    val duration: Duration,
    val labels: Map<String, String> = emptyMap()
)
