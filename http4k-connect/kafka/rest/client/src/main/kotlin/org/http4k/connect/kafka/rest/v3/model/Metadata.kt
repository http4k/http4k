package org.http4k.connect.kafka.rest.v3.model

import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Metadata(val self: Uri, val resource_name: ResourceName? = null, val next: Uri? = null)
