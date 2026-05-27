package org.http4k.connect.openfeature.endpoints

import org.http4k.connect.openfeature.OpenFeatureMoshi.json
import org.http4k.connect.openfeature.model.EvaluationContext
import org.http4k.core.Request

internal fun Request.evaluationContext() = runCatching { json<EvaluationContext>() }.getOrElse { EvaluationContext() }
