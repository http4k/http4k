package org.http4k.connect.amazon.s3.model

data class GlacierJobParameters(
    val Tier: RestoreTier
)

enum class RestoreTier {
    Standard, Bulk, Expedited
}
