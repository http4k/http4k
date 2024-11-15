package org.http4k.connect.amazon.cognito.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cognito.CognitoAction
import org.http4k.connect.amazon.cognito.model.AccessToken
import org.http4k.connect.amazon.cognito.model.SecretCode
import org.http4k.connect.amazon.cognito.model.Session
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class AssociateSoftwareToken(
    val AccessToken: AccessToken? = null,
    val Session: Session? = null
) : CognitoAction<SoftwareTokenSecret>(SoftwareTokenSecret::class)

@JsonSerializable
data class SoftwareTokenSecret(
    val SecretCode: SecretCode,
    val Session: Session
)

