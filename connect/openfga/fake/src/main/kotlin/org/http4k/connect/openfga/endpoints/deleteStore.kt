package org.http4k.connect.openfga.endpoints

import org.http4k.connect.openfga.FgaStore
import org.http4k.connect.openfga.model.StoreId
import org.http4k.core.Method.DELETE
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.routing.bind

fun deleteStore(stores: MutableMap<StoreId, FgaStore>) =
    "/stores/{store}" bind DELETE to { req ->
        stores.withStore(req) { id, _ ->
            stores.remove(id)
            Response(NO_CONTENT)
        }
    }
