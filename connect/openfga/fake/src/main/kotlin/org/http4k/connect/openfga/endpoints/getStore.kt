package org.http4k.connect.openfga.endpoints

import org.http4k.connect.openfga.FgaStore
import org.http4k.connect.openfga.OpenFgaMoshi.json
import org.http4k.connect.openfga.model.StoreId
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind

fun getStore(stores: MutableMap<StoreId, FgaStore>) =
    "/stores/{store}" bind GET to { req ->
        stores.withStore(req) { _, store -> Response(OK).json(store.store) }
    }
