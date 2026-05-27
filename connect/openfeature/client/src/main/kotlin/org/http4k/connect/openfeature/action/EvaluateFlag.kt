package org.http4k.connect.openfeature.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.kClass
import org.http4k.connect.openfeature.OpenFeatureAction
import org.http4k.connect.openfeature.OpenFeatureMoshi.json
import org.http4k.connect.openfeature.model.EvaluationContext
import org.http4k.connect.openfeature.model.EvaluationSuccess
import org.http4k.connect.openfeature.model.FlagKey
import org.http4k.core.Method.POST
import org.http4k.core.Request

@Http4kConnectAction
data class EvaluateFlag(val key: FlagKey, val context: EvaluationContext) :
    OpenFeatureAction<EvaluationSuccess>(kClass()) {
    override fun toRequest() = Request(POST, "/ofrep/v1/evaluate/flags/$key").json(context)
}
