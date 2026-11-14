package org.http4k.connect.openfga.endpoints

import org.http4k.connect.openfga.FgaStore
import org.http4k.connect.openfga.OpenFgaMoshi.json
import org.http4k.connect.openfga.model.AuthorizationModelFound
import org.http4k.connect.openfga.model.AuthorizationModelId
import org.http4k.connect.openfga.model.StoreId
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Path
import org.http4k.lens.value
import org.http4k.routing.bind

fun readAuthorizationModel(stores: MutableMap<StoreId, FgaStore>) =
    "/stores/{store}/authorization-models/{id}" bind GET to { req ->
        stores.withStore(req) { _, store ->
            val id = modelId(req)
            val model = store.models.find { it.id == id } ?: return@withStore Response(NOT_FOUND)
            Response(OK).json(AuthorizationModelFound(model))
        }
    }

private val modelId = Path.value(AuthorizationModelId).of("id")
