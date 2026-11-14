package org.http4k.connect.openfga.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class TupleKeys(val tuple_keys: List<TupleKey>)

fun List<TupleKey>.asTupleKeys() = if (isEmpty()) null else TupleKeys(this)
