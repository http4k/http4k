package org.http4k.connect.openfga.endpoints

import org.http4k.connect.openfga.FgaStore
import org.http4k.connect.openfga.OpenFgaMoshi.json
import org.http4k.connect.openfga.action.WriteBody
import org.http4k.connect.openfga.model.OnConflict.IGNORE
import org.http4k.connect.openfga.model.OpenFgaError
import org.http4k.connect.openfga.model.StoreId
import org.http4k.connect.openfga.model.TupleChange
import org.http4k.connect.openfga.model.TupleOperation.TUPLE_OPERATION_DELETE
import org.http4k.connect.openfga.model.TupleOperation.TUPLE_OPERATION_WRITE
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import java.time.Clock

fun write(clock: Clock, stores: MutableMap<StoreId, FgaStore>) =
    "/stores/{store}/write" bind POST to { req ->
        stores.withModel(req) { id, store ->
            val body = req.json<WriteBody>()
            val existing = store.tuples.map { it.key }.toSet()
            val writes = body.writes?.tuple_keys.orEmpty()
            val deletes = body.deletes?.tuple_keys.orEmpty()
            val duplicates = writes.filter { it in existing }
            val missing = deletes.filterNot { it in existing }

            when {
                duplicates.isNotEmpty() && body.writes?.on_duplicate != IGNORE ->
                    invalidWrite("cannot write a tuple which already exists: ${duplicates.first()}")

                missing.isNotEmpty() && body.deletes?.on_missing != IGNORE ->
                    invalidWrite("cannot delete a tuple which does not exist: ${missing.first()}")

                else -> {
                    val now = clock.instant()
                    val changes = (deletes - missing.toSet()).map { TupleChange(it, TUPLE_OPERATION_DELETE, now) } +
                        (writes - duplicates.toSet()).map { TupleChange(it, TUPLE_OPERATION_WRITE, now) }
                    stores[id] = store.copy(changes = store.changes + changes)
                    Response(OK).body("{}")
                }
            }
        }
    }

private fun invalidWrite(message: String) =
    Response(BAD_REQUEST).json(OpenFgaError("write_failed_due_to_invalid_input", message))
