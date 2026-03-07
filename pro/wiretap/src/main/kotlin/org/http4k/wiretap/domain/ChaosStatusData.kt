package org.http4k.wiretap.domain

data class ChaosStatusData(
    val inboundActive: Boolean,
    val inboundDescription: String,
    val outboundActive: Boolean,
    val outboundDescription: String
)
