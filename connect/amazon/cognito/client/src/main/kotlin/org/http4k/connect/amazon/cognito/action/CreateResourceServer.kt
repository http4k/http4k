package org.http4k.connect.amazon.cognito.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cognito.CognitoAction
import org.http4k.connect.amazon.cognito.model.UserPoolId
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class CreateResourceServer(
    val UserPoolId: UserPoolId,
    val Name: String,
    val Identifier: String,
    val Scopes: List<Scope>? = null
) : CognitoAction<CreatedResourceServer>(CreatedResourceServer::class)

@JsonSerializable
data class Scope(
    val ScopeName: String?,
    val ScopeDescription: String?
)

@JsonSerializable
data class ResourceServer(
    val UserPoolId: UserPoolId,
    val Name: String,
    val Identifier: String,
    val Scopes: List<Scope>? = null
)

@JsonSerializable
data class CreatedResourceServer(val ResourceServer: ResourceServer)
