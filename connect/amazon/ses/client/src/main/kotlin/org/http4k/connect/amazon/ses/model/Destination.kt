package org.http4k.connect.amazon.ses.model

import com.squareup.moshi.Json
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Destination(
    @Json(name = "ToAddresses") val toAddresses: Set<EmailAddress>? = null,
    @Json(name = "CcAddresses") val ccAddresses: Set<EmailAddress>? = null,
    @Json(name = "BccAddresses") val bccAddresses: Set<EmailAddress>? = null
)

