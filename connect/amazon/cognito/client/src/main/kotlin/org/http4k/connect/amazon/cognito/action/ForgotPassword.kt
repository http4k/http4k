package org.http4k.connect.amazon.cognito.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cognito.CognitoAction
import org.http4k.connect.amazon.cognito.model.AnalyticsMetadata
import org.http4k.connect.amazon.cognito.model.ClientId
import org.http4k.connect.amazon.cognito.model.CodeDeliveryDetails
import org.http4k.connect.amazon.cognito.model.SecretHash
import org.http4k.connect.amazon.cognito.model.UserContextData
import org.http4k.connect.amazon.core.model.Username
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Response
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class ForgotPassword(
    val Username: Username,
    val ClientId: ClientId,
    val AnalyticsMetadata: AnalyticsMetadata? = null,
    val ClientMetadata: Map<String, String>? = null,
    val SecretHash: SecretHash? = null,
    val UserContextData: UserContextData? = null,
) : CognitoAction<Unit>(Unit::class) {
    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(Unit)
            else -> Failure(asRemoteFailure(this))
        }
    }
}

@JsonSerializable
data class ForgotPasswordResponse(val CodeDeliveryDetails: CodeDeliveryDetails?)
