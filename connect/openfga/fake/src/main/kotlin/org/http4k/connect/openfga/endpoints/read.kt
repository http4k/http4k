package org.http4k.connect.openfga.endpoints

import org.http4k.connect.openfga.FgaStore
import org.http4k.connect.openfga.OpenFgaMoshi.json
import org.http4k.connect.openfga.action.ReadBody
import org.http4k.connect.openfga.model.StoreId
import org.http4k.connect.openfga.model.TupleFilter
import org.http4k.connect.openfga.model.TupleKey
import org.http4k.connect.openfga.model.Tuples
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind

fun read(stores: MutableMap<StoreId, FgaStore>) =
    "/stores/{store}/read" bind POST to { req ->
        stores.withStore(req) { _, store ->
            val filter = req.json<ReadBody>().tuple_key ?: TupleFilter()
            Response(OK).json(Tuples(store.tuples.filter { filter.matches(it.key) }))
        }
    }

private fun TupleFilter.matches(key: TupleKey): Boolean {
    val obj = obj
    return (user == null || user == key.user) &&
        (relation == null || relation == key.relation) &&
        (obj == null || obj == key.obj || (obj.value.endsWith(':') && key.obj.value.startsWith(obj.value)))
}
