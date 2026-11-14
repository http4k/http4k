package org.http4k.connect.openfga.endpoints

import org.http4k.connect.openfga.FgaStore
import org.http4k.connect.openfga.OpenFgaMoshi.json
import org.http4k.connect.openfga.action.CheckBody
import org.http4k.connect.openfga.model.CheckResult
import org.http4k.connect.openfga.model.StoreId
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind

fun check(stores: MutableMap<StoreId, FgaStore>) =
    "/stores/{store}/check" bind POST to { req ->
        val body = req.json<CheckBody>()
        stores.withEvaluation(req, body.authorization_model_id) { evaluation ->
            val key = body.tuple_key
            Response(OK).json(CheckResult(evaluation.with(body.contextual_tuples).check(key.user, key.relation, key.obj)))
        }
    }
