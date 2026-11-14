@file:OptIn(ExperimentalKotshiApi::class)

package org.http4k.connect.openfga.model

import se.ansman.kotshi.ExperimentalKotshiApi
import se.ansman.kotshi.JsonProperty
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class TupleKey(
    val user: User,
    val relation: Relation,
    @JsonProperty("object")
    val obj: FgaObject
)
