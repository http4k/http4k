package org.http4k.connect.openfga.endpoints

import org.http4k.connect.openfga.FgaStore
import org.http4k.connect.openfga.OpenFgaMoshi.json
import org.http4k.connect.openfga.model.StoreId
import org.http4k.connect.openfga.model.StoreName
import org.http4k.connect.openfga.model.Stores
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Query
import org.http4k.lens.value
import org.http4k.routing.bind

fun listStores(stores: MutableMap<StoreId, FgaStore>) =
    "/stores" bind GET to { req ->
        val name = nameQuery(req)
        Response(OK).json(Stores(stores.values.map { it.store }.filter { name == null || it.name == name }))
    }

private val nameQuery = Query.value(StoreName).optional("name")
