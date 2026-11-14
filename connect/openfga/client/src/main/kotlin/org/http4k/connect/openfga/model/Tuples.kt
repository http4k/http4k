package org.http4k.connect.openfga.model

import se.ansman.kotshi.JsonSerializable
import java.time.Instant

@JsonSerializable
data class Tuples(val tuples: List<Tuple>)

@JsonSerializable
data class Tuple(val key: TupleKey, val timestamp: Instant)
