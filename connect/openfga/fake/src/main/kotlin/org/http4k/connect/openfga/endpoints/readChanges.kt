package org.http4k.connect.openfga.endpoints

import org.http4k.connect.openfga.FgaStore
import org.http4k.connect.openfga.OpenFgaMoshi.json
import org.http4k.connect.openfga.model.ContinuationToken
import org.http4k.connect.openfga.model.StoreId
import org.http4k.connect.openfga.model.TupleChanges
import org.http4k.connect.openfga.model.TypeName
import org.http4k.connect.openfga.type
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.value
import org.http4k.routing.bind

fun readChanges(stores: MutableMap<StoreId, FgaStore>) =
    "/stores/{store}/changes" bind GET to { req ->
        stores.withStore(req) { _, store ->
            val from = continuationToken(req) ?: 0
            val type = typeQuery(req)
            val changes = store.changes.drop(from).filter { type == null || it.tuple_key.obj.type == type }
            Response(OK).json(TupleChanges(changes, ContinuationToken.of(store.changes.size.toString())))
        }
    }

private val continuationToken = Query.int().optional("continuation_token")

private val typeQuery = Query.value(TypeName).optional("type")
