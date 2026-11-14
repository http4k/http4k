package org.http4k.connect.openfga.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.kClass
import org.http4k.connect.openfga.OpenFgaAction
import org.http4k.connect.openfga.OpenFgaMoshi.json
import org.http4k.connect.openfga.model.AuthorizationModelId
import org.http4k.connect.openfga.model.CheckResult
import org.http4k.connect.openfga.model.ConsistencyPreference
import org.http4k.connect.openfga.model.StoreId
import org.http4k.connect.openfga.model.TupleKey
import org.http4k.connect.openfga.model.TupleKeys
import org.http4k.connect.openfga.model.asTupleKeys
import org.http4k.core.Method.POST
import org.http4k.core.Request
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class Check(
    val store: StoreId,
    val tupleKey: TupleKey,
    val contextualTuples: List<TupleKey> = emptyList(),
    val authorizationModel: AuthorizationModelId? = null,
    val consistency: ConsistencyPreference? = null
) : OpenFgaAction<CheckResult>(kClass()) {
    override fun toRequest() = Request(POST, "/stores/$store/check")
        .json(CheckBody(tupleKey, contextualTuples.asTupleKeys(), authorizationModel, consistency))
}

@JsonSerializable
data class CheckBody(
    val tuple_key: TupleKey,
    val contextual_tuples: TupleKeys? = null,
    val authorization_model_id: AuthorizationModelId? = null,
    val consistency: ConsistencyPreference? = null
)
