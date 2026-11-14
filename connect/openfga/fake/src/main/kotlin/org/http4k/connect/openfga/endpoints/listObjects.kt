package org.http4k.connect.openfga.endpoints

import org.http4k.connect.openfga.FgaStore
import org.http4k.connect.openfga.OpenFgaMoshi
import org.http4k.connect.openfga.OpenFgaMoshi.json
import org.http4k.connect.openfga.action.ListObjectsBody
import org.http4k.connect.openfga.action.StreamedObject
import org.http4k.connect.openfga.action.StreamedObjectResult
import org.http4k.connect.openfga.model.FgaObject
import org.http4k.connect.openfga.model.Objects
import org.http4k.connect.openfga.model.StoreId
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind

fun listObjects(stores: MutableMap<StoreId, FgaStore>) =
    "/stores/{store}/list-objects" bind POST to { req ->
        stores.listingObjects(req) { Response(OK).json(Objects(it)) }
    }

fun streamedListObjects(stores: MutableMap<StoreId, FgaStore>) =
    "/stores/{store}/streamed-list-objects" bind POST to { req ->
        stores.listingObjects(req) { objects ->
            Response(OK).body(objects.joinToString("\n") { OpenFgaMoshi.asFormatString(StreamedObject(StreamedObjectResult(it))) })
        }
    }

private fun MutableMap<StoreId, FgaStore>.listingObjects(req: Request, fn: (List<FgaObject>) -> Response): Response {
    val body = req.json<ListObjectsBody>()
    return withEvaluation(req, body.authorization_model_id) { evaluation ->
        fn(evaluation.with(body.contextual_tuples).listObjects(body.type, body.relation, body.user))
    }
}
