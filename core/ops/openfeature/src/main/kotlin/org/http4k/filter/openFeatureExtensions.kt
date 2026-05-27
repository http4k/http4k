package org.http4k.filter

import dev.openfeature.sdk.EvaluationContext
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.lens.RequestKey

val OPENFEATURE_CONTEXT_KEY = RequestKey.required<EvaluationContext>("HTTP4K_OPENFEATURE_CONTEXT")

fun ServerFilters.PopulateOpenFeatureContext(toContext: (Request) -> EvaluationContext) = Filter { next ->
    { req -> next(req.with(OPENFEATURE_CONTEXT_KEY of toContext(req))) }
}
