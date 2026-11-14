package org.http4k.connect.openfga.endpoints

import org.http4k.connect.openfga.FgaStore
import org.http4k.connect.openfga.OpenFgaMoshi.json
import org.http4k.connect.openfga.action.CreateStore
import org.http4k.connect.openfga.model.Store
import org.http4k.connect.openfga.model.StoreId
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.CREATED
import org.http4k.routing.bind
import java.time.Clock

fun createStore(clock: Clock, nextId: () -> String, stores: MutableMap<StoreId, FgaStore>) =
    "/stores" bind POST to { req ->
        val now = clock.instant()
        val store = Store(StoreId.of(nextId()), req.json<CreateStore>().name, now, now)
        stores[store.id] = FgaStore(store)
        Response(CREATED).json(store)
    }
