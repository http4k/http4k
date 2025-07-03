package org.http4k.connect.amazon.route53.model

import dev.forkhandles.result4k.asFailure
import org.http4k.core.Response
import org.http4k.core.Status
import java.util.UUID

data class FakeRoute53Error(
    val status: Status,
    val error: ErrorResponse.Error
) {
    fun toResponse() =
        Response(status).body(ErrorResponse(UUID.randomUUID(), error).toXml())
}

fun noSuchHostedZone(hostedZoneId: HostedZoneId) = FakeRoute53Error(
    status = Status.NOT_FOUND,
    error = ErrorResponse.Error("Sender", "NoSuchHostedZone", "No hosted zone found with ID: $hostedZoneId")
).asFailure()

fun invalidChangeBatch(message: String) = FakeRoute53Error(
    status = Status.BAD_REQUEST,
    error = ErrorResponse.Error("Sender", "InvalidChangeBatch", message)
).asFailure()

fun invalidInput(message: String) = FakeRoute53Error(
    status = Status.BAD_REQUEST,
    error = ErrorResponse.Error("Sender", "InvalidInput", message)
).asFailure()

fun hostedZoneNotEmpty() = FakeRoute53Error(
    status = Status.BAD_REQUEST,
    error = ErrorResponse.Error(
        type = "Sender",
        code = "HostedZoneNotEmpty",
        message = "The specified hosted zone contains non-required resource record sets and so cannot be deleted."
    )
).asFailure()
