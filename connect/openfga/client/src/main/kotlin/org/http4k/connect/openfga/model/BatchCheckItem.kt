package org.http4k.connect.openfga.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class BatchCheckItem(
    val tuple_key: TupleKey,
    val correlation_id: CorrelationId,
    val contextual_tuples: TupleKeys? = null
)

@JsonSerializable
data class BatchCheckResults(val result: Map<CorrelationId, CheckResult>)
