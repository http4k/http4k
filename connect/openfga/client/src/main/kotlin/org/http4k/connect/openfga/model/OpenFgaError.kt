package org.http4k.connect.openfga.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class OpenFgaError(val code: String, val message: String)
