package org.http4k.connect.amazon.cognito.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cognito.CognitoAction
import org.http4k.connect.amazon.cognito.model.AnalyticsMetadata
import org.http4k.connect.amazon.cognito.model.ClientId
import org.http4k.connect.amazon.cognito.model.ConfirmationCode
import org.http4k.connect.amazon.cognito.model.SecretHash
import org.http4k.connect.amazon.cognito.model.UserContextData
import org.http4k.connect.amazon.core.model.Password
import org.http4k.connect.amazon.core.model.Username
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Response
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class ConfirmForgotPassword(
    val ClientId: ClientId,
    val Username: Username,
    val ConfirmationCode: ConfirmationCode,
    val Password: Password,
    val SecretHash: SecretHash? = null,
    val ClientMetadata: Map<String, String> = emptyMap(),
    val AnalyticsMetadata: AnalyticsMetadata? = null,
    val UserContextData: UserContextData? = null
) : CognitoAction<Unit>(Unit::class) {
    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(Unit)
            else -> Failure(asRemoteFailure(this))
        }
    }
}

