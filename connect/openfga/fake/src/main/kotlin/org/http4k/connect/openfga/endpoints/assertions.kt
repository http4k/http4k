package org.http4k.connect.openfga.endpoints

import org.http4k.connect.openfga.FgaStore
import org.http4k.connect.openfga.OpenFgaMoshi.json
import org.http4k.connect.openfga.action.WriteAssertionsBody
import org.http4k.connect.openfga.model.Assertions
import org.http4k.connect.openfga.model.AuthorizationModelId
import org.http4k.connect.openfga.model.StoreId
import org.http4k.core.Method.GET
import org.http4k.core.Method.PUT
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Path
import org.http4k.lens.value
import org.http4k.routing.bind

fun writeAssertions(stores: MutableMap<StoreId, FgaStore>) =
    "/stores/{store}/assertions/{model}" bind PUT to { req ->
        stores.withStore(req) { id, store ->
            stores[id] = store.copy(assertions = store.assertions + (modelId(req) to req.json<WriteAssertionsBody>().assertions))
            Response(NO_CONTENT)
        }
    }

fun readAssertions(stores: MutableMap<StoreId, FgaStore>) =
    "/stores/{store}/assertions/{model}" bind GET to { req ->
        stores.withStore(req) { _, store ->
            val model = modelId(req)
            Response(OK).json(Assertions(model, store.assertions[model].orEmpty()))
        }
    }

private val modelId = Path.value(AuthorizationModelId).of("model")
