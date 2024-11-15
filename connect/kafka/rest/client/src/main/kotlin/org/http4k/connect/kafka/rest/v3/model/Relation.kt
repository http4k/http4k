package org.http4k.connect.kafka.rest.v3.model

import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Relation(val related: Uri)
