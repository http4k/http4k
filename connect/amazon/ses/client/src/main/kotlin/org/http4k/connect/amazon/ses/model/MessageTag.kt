package org.http4k.connect.amazon.ses.model

import com.squareup.moshi.Json
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class MessageTag(
    @Json(name = "Name") val name: String,
    @Json(name = "Value") val value: String
)
