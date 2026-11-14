package org.http4k.connect.openfga.endpoints

import org.http4k.connect.openfga.FgaStore
import org.http4k.connect.openfga.OpenFgaMoshi.json
import org.http4k.connect.openfga.action.WriteAuthorizationModelBody
import org.http4k.connect.openfga.model.AuthorizationModel
import org.http4k.connect.openfga.model.AuthorizationModelId
import org.http4k.connect.openfga.model.AuthorizationModelWritten
import org.http4k.connect.openfga.model.StoreId
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.CREATED
import org.http4k.routing.bind

fun writeAuthorizationModel(nextId: () -> String, stores: MutableMap<StoreId, FgaStore>) =
    "/stores/{store}/authorization-models" bind POST to { req ->
        stores.withStore(req) { id, store ->
            val body = req.json<WriteAuthorizationModelBody>()
            val model = AuthorizationModel(AuthorizationModelId.of(nextId()), body.schema_version, body.type_definitions)
            stores[id] = store.copy(models = store.models + model)
            Response(CREATED).json(AuthorizationModelWritten(model.id))
        }
    }
