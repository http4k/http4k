package org.http4k.connect.github.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Email(
    val email: String,
    val verified: Boolean,
    val primary: Boolean,
    val visibility: String?
)
