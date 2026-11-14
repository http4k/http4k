package org.http4k.connect.openfga.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class AuthorizationModel(
    val id: AuthorizationModelId,
    val schema_version: String,
    val type_definitions: List<TypeDefinition>
)

@JsonSerializable
data class AuthorizationModels(val authorization_models: List<AuthorizationModel>)

@JsonSerializable
data class AuthorizationModelFound(val authorization_model: AuthorizationModel)
