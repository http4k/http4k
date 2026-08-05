package org.http4k.connect.amazon.cognito.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cognito.CognitoAction
import org.http4k.connect.amazon.cognito.model.AccessToken
import org.http4k.connect.amazon.cognito.model.EmailMfaSettingsType
import org.http4k.connect.amazon.cognito.model.SMSMfaSettingsType
import org.http4k.connect.amazon.cognito.model.SoftwareTokenMfaSettingsType
import org.http4k.connect.amazon.cognito.model.WebAuthnMfaSettingsType
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Response
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class SetUserMFAPreference(
    val AccessToken: AccessToken,
    val EmailMfaSettings: EmailMfaSettingsType? = null,
    val SMSMfaSettings: SMSMfaSettingsType? = null,
    val SoftwareTokenMfaSettings: SoftwareTokenMfaSettingsType? = null,
    val WebAuthnMfaSettings: WebAuthnMfaSettingsType? = null
) : CognitoAction<Unit>(Unit::class) {
    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(Unit)
            else -> Failure(asRemoteFailure(this))
        }
    }
}
