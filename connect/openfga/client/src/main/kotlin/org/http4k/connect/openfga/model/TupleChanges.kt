package org.http4k.connect.openfga.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class TupleChanges(val changes: List<TupleChange>, val continuation_token: ContinuationToken? = null)
