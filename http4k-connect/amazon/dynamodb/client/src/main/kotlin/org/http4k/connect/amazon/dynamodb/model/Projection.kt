package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Projection(
    val NonKeyAttributes: List<AttributeName>? = null,
    val ProjectionType: ProjectionType? = null
) {
    companion object {
        val all = Projection(ProjectionType = ProjectionType.ALL)
    }
}
