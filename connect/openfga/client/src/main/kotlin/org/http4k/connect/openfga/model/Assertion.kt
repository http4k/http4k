package org.http4k.connect.openfga.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Assertion(val tuple_key: TupleKey, val expectation: Boolean)

@JsonSerializable
data class Assertions(val authorization_model_id: AuthorizationModelId, val assertions: List<Assertion> = emptyList())
