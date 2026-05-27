package org.http4k.filter

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import dev.openfeature.sdk.EvaluationContext
import org.http4k.connect.openfeature.OpenFeature
import org.http4k.connect.openfeature.action.EvaluateAllFlags
import org.http4k.connect.openfeature.model.EvaluationResult
import org.http4k.connect.openfeature.model.FlagKey
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.lens.RequestKey
import org.http4k.ops.openfeature.toHttp4k

data class OpenFeatureSnapshot(
    val context: EvaluationContext,
    val flags: Map<FlagKey, EvaluationResult>
)

val OPENFEATURE_CONTEXT_KEY = RequestKey.required<OpenFeatureSnapshot>("HTTP4K_OPENFEATURE")

fun ServerFilters.PopulateOpenFeatureContext(
    client: OpenFeature,
    toContext: (Request) -> EvaluationContext
) = Filter { next ->
    { req ->
        val context = toContext(req)
        next(req.with(OPENFEATURE_CONTEXT_KEY of OpenFeatureSnapshot(context,
            client(EvaluateAllFlags(context.toHttp4k()))
                .map { it.flags.associateBy { it.key } }
                .recover { emptyMap() })))
    }
}
