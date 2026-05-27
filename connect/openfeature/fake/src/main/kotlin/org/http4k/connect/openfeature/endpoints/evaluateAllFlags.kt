package org.http4k.connect.openfeature.endpoints

import org.http4k.connect.openfeature.FlagRule
import org.http4k.connect.openfeature.OpenFeatureMoshi.json
import org.http4k.connect.openfeature.model.BulkEvaluationSuccess
import org.http4k.connect.openfeature.model.EvaluationResult
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind

fun evaluateAllFlags(rules: List<FlagRule>) =
    "/ofrep/v1/evaluate/flags" bind POST to { req ->
        val context = req.evaluationContext()
        val results = rules.map { it.key }.distinct().map { key ->
            val matched = rules.firstMatch(key, context)
            EvaluationResult(key = key, value = matched?.value, reason = matched?.reason)
        }
        Response(OK).json(BulkEvaluationSuccess(results))
    }
