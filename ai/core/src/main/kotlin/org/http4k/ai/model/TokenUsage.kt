package org.http4k.ai.model

data class TokenUsage(
    val input: Int? = null,
    val output: Int? = null,
    val total: Int? = (input ?: 0) + (output ?: 0)
)
