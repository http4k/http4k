package org.http4k.connect.amazon.cognito.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cognito.CognitoAction
import org.http4k.connect.amazon.cognito.model.AccessToken
import org.http4k.connect.amazon.cognito.model.Session
import org.http4k.connect.amazon.cognito.model.UserCode
import org.http4k.connect.amazon.cognito.model.VerifyStatus
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class VerifySoftwareToken(
    val UserCode: UserCode,
    val AccessToken: AccessToken? = null,
    val Session: Session? = null,
    val FriendlyDeviceName: String? = null
) : CognitoAction<VerifyResult>(VerifyResult::class)

@JsonSerializable
data class VerifyResult(
    val Session: Session,
    val Status: VerifyStatus
)
