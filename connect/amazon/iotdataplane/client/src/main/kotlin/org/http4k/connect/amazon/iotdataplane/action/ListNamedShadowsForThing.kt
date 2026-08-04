package org.http4k.connect.amazon.iotdataplane.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.iotdataplane.IotDataPlaneAction
import org.http4k.connect.amazon.iotdataplane.IotDataPlaneMoshi
import org.http4k.connect.amazon.iotdataplane.model.ShadowName
import org.http4k.connect.amazon.iotdataplane.model.ThingName
import org.http4k.connect.asRemoteFailure
import org.http4k.connect.model.Timestamp
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class ListNamedShadowsForThing(
    val thingName: ThingName,
    val nextToken: String? = null,
    val pageSize: Int? = null
) : IotDataPlaneAction<NamedShadows> {

    override fun toRequest() = queryParameters()
        .fold(Request(GET, uri())) { request, (name, value) -> request.query(name, value) }

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(IotDataPlaneMoshi.asA<NamedShadows>(bodyString()))
            else -> Failure(asRemoteFailure(this))
        }
    }

    private fun uri() = Uri.of("").path("/api/things/shadow/ListNamedShadowsForThing/${thingName.value}")

    private fun queryParameters() = listOfNotNull(
        nextToken?.let { "nextToken" to it },
        pageSize?.let { "pageSize" to it.toString() }
    )
}

@JsonSerializable
data class NamedShadows(
    val results: List<ShadowName> = emptyList(),
    val nextToken: String? = null,
    val timestamp: Timestamp? = null
)
