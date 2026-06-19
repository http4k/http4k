package org.http4k.connect.github.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Plan(
    val name: String,
    val space: Int,
    val private_repos: Int,
    val collaborators: Int
)
