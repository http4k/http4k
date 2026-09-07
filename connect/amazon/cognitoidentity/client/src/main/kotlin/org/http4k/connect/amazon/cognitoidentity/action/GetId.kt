package org.http4k.connect.amazon.cognitoidentity.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cognitoidentity.CognitoIdentityAction
import org.http4k.connect.amazon.cognitoidentity.model.IdentityId
import org.http4k.connect.amazon.cognitoidentity.model.IdentityPoolId
import org.http4k.connect.amazon.core.model.AwsAccount
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class GetId(
    val IdentityPoolId: IdentityPoolId,
    val AccountId: AwsAccount? = null,
    val Logins: Map<String, String>? = null,
) : CognitoIdentityAction<Identity>(Identity::class)

@JsonSerializable
data class Identity(
    val IdentityId: IdentityId,
)
