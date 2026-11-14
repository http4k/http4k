package org.http4k.connect.openfga.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.openfga.OpenFgaAction
import org.http4k.connect.openfga.OpenFgaMoshi.json
import org.http4k.connect.openfga.model.OnConflict
import org.http4k.connect.openfga.model.StoreId
import org.http4k.connect.openfga.model.TupleKey
import org.http4k.core.Method.POST
import org.http4k.core.Request
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class Write(
    val store: StoreId,
    val writes: List<TupleKey> = emptyList(),
    val deletes: List<TupleKey> = emptyList(),
    val onDuplicate: OnConflict? = null,
    val onMissing: OnConflict? = null
) : OpenFgaAction<Unit>(Unit::class) {
    override fun toRequest() = Request(POST, "/stores/$store/write").json(
        WriteBody(
            if (writes.isEmpty()) null else Writes(writes, onDuplicate),
            if (deletes.isEmpty()) null else Deletes(deletes, onMissing)
        )
    )
}

@JsonSerializable
data class WriteBody(val writes: Writes? = null, val deletes: Deletes? = null)

@JsonSerializable
data class Writes(val tuple_keys: List<TupleKey>, val on_duplicate: OnConflict? = null)

@JsonSerializable
data class Deletes(val tuple_keys: List<TupleKey>, val on_missing: OnConflict? = null)
