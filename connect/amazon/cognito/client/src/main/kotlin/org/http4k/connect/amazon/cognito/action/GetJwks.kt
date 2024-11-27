package org.http4k.connect.amazon.cognito.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cognito.CognitoAction
import org.http4k.connect.amazon.cognito.model.Jwks
import org.http4k.connect.amazon.cognito.model.UserPoolId
import org.http4k.connect.kClass
import org.http4k.core.Method.GET
import org.http4k.core.Request
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class GetJwks(val UserPoolId: UserPoolId) : CognitoAction<Jwks>(kClass()) {
    override fun toRequest() = Request(GET, "/${UserPoolId}/.well-known/jwks.json")
}
