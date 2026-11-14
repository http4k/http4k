package org.http4k.connect.openfga.endpoints

import org.http4k.connect.openfga.FgaStore
import org.http4k.connect.openfga.OpenFgaMoshi.json
import org.http4k.connect.openfga.action.BatchCheckBody
import org.http4k.connect.openfga.model.BatchCheckResults
import org.http4k.connect.openfga.model.CheckResult
import org.http4k.connect.openfga.model.StoreId
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind

fun batchCheck(stores: MutableMap<StoreId, FgaStore>) =
    "/stores/{store}/batch-check" bind POST to { req ->
        val body = req.json<BatchCheckBody>()
        stores.withEvaluation(req, body.authorization_model_id) { evaluation ->
            val results = body.checks.associate {
                it.correlation_id to CheckResult(evaluation.with(it.contextual_tuples).check(it.tuple_key.user, it.tuple_key.relation, it.tuple_key.obj))
            }
            Response(OK).json(BatchCheckResults(results))
        }
    }
