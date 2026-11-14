package org.http4k.connect.openfga.model

import se.ansman.kotshi.JsonSerializable
import java.time.Instant

@JsonSerializable
data class TupleChange(val tuple_key: TupleKey, val operation: TupleOperation, val timestamp: Instant)

enum class TupleOperation {
    TUPLE_OPERATION_WRITE, TUPLE_OPERATION_DELETE
}
