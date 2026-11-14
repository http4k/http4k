@file:OptIn(ExperimentalKotshiApi::class)

package org.http4k.connect.openfga.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.asRemoteFailure
import org.http4k.connect.kClass
import org.http4k.connect.openfga.OpenFgaAction
import org.http4k.connect.openfga.OpenFgaMoshi
import org.http4k.connect.openfga.OpenFgaMoshi.json
import org.http4k.connect.openfga.model.AuthorizationModelId
import org.http4k.connect.openfga.model.ConsistencyPreference
import org.http4k.connect.openfga.model.FgaObject
import org.http4k.connect.openfga.model.Relation
import org.http4k.connect.openfga.model.StoreId
import org.http4k.connect.openfga.model.TupleKey
import org.http4k.connect.openfga.model.TypeName
import org.http4k.connect.openfga.model.User
import org.http4k.connect.openfga.model.asTupleKeys
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import se.ansman.kotshi.ExperimentalKotshiApi
import se.ansman.kotshi.JsonProperty
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class StreamedListObjects(
    val store: StoreId,
    val type: TypeName,
    val relation: Relation,
    val user: User,
    val contextualTuples: List<TupleKey> = emptyList(),
    val authorizationModel: AuthorizationModelId? = null,
    val consistency: ConsistencyPreference? = null
) : OpenFgaAction<Sequence<FgaObject>>(kClass()) {
    override fun toRequest() = Request(POST, "/stores/$store/streamed-list-objects")
        .json(ListObjectsBody(type, relation, user, contextualTuples.asTupleKeys(), authorizationModel, consistency))

    override fun toResult(response: Response): Result<Sequence<FgaObject>, RemoteFailure> = when {
        response.status.successful -> Success(
            response.body.stream.bufferedReader().lineSequence()
                .filter { it.isNotBlank() }
                .map { OpenFgaMoshi.asA<StreamedObject>(it).result.obj }
                .constrainOnce()
        )

        else -> Failure(asRemoteFailure(response))
    }
}

@JsonSerializable
data class StreamedObject(val result: StreamedObjectResult)

@JsonSerializable
data class StreamedObjectResult(@JsonProperty("object") val obj: FgaObject)
