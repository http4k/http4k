package org.http4k.connect.amazon.iotdataplane.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.iotdataplane.IotDataPlaneAction
import org.http4k.connect.amazon.iotdataplane.IotDataPlaneMoshi
import org.http4k.connect.amazon.iotdataplane.model.TopicName
import org.http4k.connect.asRemoteFailure
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.TimestampMillis
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class GetRetainedMessage(val topic: TopicName) : IotDataPlaneAction<RetainedMessage> {

    override fun toRequest() = Request(GET, uri())

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(IotDataPlaneMoshi.asA<RetainedMessage>(bodyString()))
            else -> Failure(asRemoteFailure(this))
        }
    }

    private fun uri() = Uri.of("").path("/retainedMessage/${topic.value}")
}

/**
 * [userProperties] stays as the base64 JSON AWS sends. Decoding it here would turn a successful call
 * into a parse failure if AWS ever shaped it differently.
 */
@JsonSerializable
data class RetainedMessage(
    val topic: TopicName,
    val lastModifiedTime: TimestampMillis,
    val qos: Int = 0,
    val payload: Base64Blob? = null,
    val userProperties: Base64Blob? = null
)
