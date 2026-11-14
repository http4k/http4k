package org.http4k.connect.openfga.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.kClass
import org.http4k.connect.openfga.OpenFgaAction
import org.http4k.connect.openfga.OpenFgaMoshi.json
import org.http4k.connect.openfga.model.AuthorizationModelId
import org.http4k.connect.openfga.model.BatchCheckItem
import org.http4k.connect.openfga.model.BatchCheckResults
import org.http4k.connect.openfga.model.ConsistencyPreference
import org.http4k.connect.openfga.model.StoreId
import org.http4k.core.Method.POST
import org.http4k.core.Request
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class BatchCheck(
    val store: StoreId,
    val checks: List<BatchCheckItem>,
    val authorizationModel: AuthorizationModelId? = null,
    val consistency: ConsistencyPreference? = null
) : OpenFgaAction<BatchCheckResults>(kClass()) {
    override fun toRequest() = Request(POST, "/stores/$store/batch-check")
        .json(BatchCheckBody(checks, authorizationModel, consistency))
}

@JsonSerializable
data class BatchCheckBody(
    val checks: List<BatchCheckItem>,
    val authorization_model_id: AuthorizationModelId? = null,
    val consistency: ConsistencyPreference? = null
)
