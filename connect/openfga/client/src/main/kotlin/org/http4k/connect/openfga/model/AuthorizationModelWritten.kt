package org.http4k.connect.openfga.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class AuthorizationModelWritten(val authorization_model_id: AuthorizationModelId)
