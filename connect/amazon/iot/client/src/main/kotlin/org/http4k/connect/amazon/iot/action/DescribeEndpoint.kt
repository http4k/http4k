package org.http4k.connect.amazon.iot.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.iot.IotAction
import org.http4k.connect.amazon.iot.IotMoshi
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

/**
 * Returns the account-specific IoT endpoint. Valid [endpointType] values are `iot:Data`,
 * `iot:Data-ATS`, `iot:CredentialProvider` and `iot:Jobs`.
 */
@Http4kConnectAction
data class DescribeEndpoint(
    val endpointType: String? = null,
) : IotAction<IotEndpoint> {

    override fun toRequest() = Request(GET, Uri.of("").path("/endpoint"))
        .let { request -> endpointType?.let { request.query("endpointType", it) } ?: request }

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(IotMoshi.asA<IotEndpoint>(bodyString()))
            else -> Failure(asRemoteFailure(this))
        }
    }
}

@JsonSerializable
data class IotEndpoint(
    val endpointAddress: String,
)
