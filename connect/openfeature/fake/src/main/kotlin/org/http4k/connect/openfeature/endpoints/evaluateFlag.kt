package org.http4k.connect.openfeature.endpoints

import org.http4k.connect.openfeature.FlagRule
import org.http4k.connect.openfeature.OpenFeatureMoshi.json
import org.http4k.connect.openfeature.model.ErrorCode.FLAG_NOT_FOUND
import org.http4k.connect.openfeature.model.EvaluationFailure
import org.http4k.connect.openfeature.model.EvaluationSuccess
import org.http4k.connect.openfeature.model.FlagKey
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.path

fun evaluateFlag(rules: List<FlagRule>) =
    "/ofrep/v1/evaluate/flags/{key}" bind POST to { req ->
        val key = FlagKey.of(req.path("key")!!)
        val matched = rules.firstMatch(key, req.evaluationContext())
        when {
            matched != null -> Response(OK).json(EvaluationSuccess(key, matched.value, reason = matched.reason))
            else -> Response(NOT_FOUND).json(EvaluationFailure(key, FLAG_NOT_FOUND, "Flag '$key' not found"))
        }
    }
