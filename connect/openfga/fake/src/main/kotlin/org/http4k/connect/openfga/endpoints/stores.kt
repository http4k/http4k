package org.http4k.connect.openfga.endpoints

import org.http4k.connect.openfga.Evaluation
import org.http4k.connect.openfga.FgaStore
import org.http4k.connect.openfga.OpenFgaMoshi.json
import org.http4k.connect.openfga.model.AuthorizationModelId
import org.http4k.connect.openfga.model.OpenFgaError
import org.http4k.connect.openfga.model.StoreId
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.lens.Path
import org.http4k.lens.value

fun MutableMap<StoreId, FgaStore>.withStore(request: Request, fn: (StoreId, FgaStore) -> Response): Response {
    val id = storeId(request)
    val store = this[id] ?: return Response(NOT_FOUND)
    return fn(id, store)
}

fun MutableMap<StoreId, FgaStore>.withModel(request: Request, fn: (StoreId, FgaStore) -> Response): Response {
    val id = storeId(request)
    val store = this[id]?.takeIf { it.models.isNotEmpty() } ?: return noModel(id)
    return fn(id, store)
}

fun MutableMap<StoreId, FgaStore>.withEvaluation(
    request: Request,
    model: AuthorizationModelId?,
    fn: (Evaluation) -> Response
): Response {
    val id = storeId(request)
    val evaluation = this[id]?.evaluation(model) ?: return noModel(id)
    return fn(evaluation)
}

private fun noModel(id: StoreId) = Response(BAD_REQUEST)
    .json(OpenFgaError("latest_authorization_model_not_found", "No authorization models found for store '$id'"))

private val storeId = Path.value(StoreId).of("store")
